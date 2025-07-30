#!/bin/bash

echo "🔍 Checking REPLTILE extension propagation status..."
echo "================================================="
echo ""

EXTENSION_ID="repltile.repltile-clojure-repl"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "📋 Extension ID: $EXTENSION_ID"
echo "🕒 Check time: $(date)"
echo ""

# 1. Check VS Code Marketplace API
echo "1️⃣ Checking VS Code Marketplace API..."
MARKETPLACE_RESPONSE=$(curl -s "https://marketplace.visualstudio.com/items?itemName=$EXTENSION_ID" | grep -o "REPLTILE - Interactive Clojure REPL" | head -1)

if [ ! -z "$MARKETPLACE_RESPONSE" ]; then
    echo -e "${GREEN}✅ VS Code Marketplace: AVAILABLE${NC}"
    echo "   📄 Page: https://marketplace.visualstudio.com/items?itemName=$EXTENSION_ID"
else
    echo -e "${YELLOW}⏳ VS Code Marketplace: PROPAGATING${NC}"
fi

echo ""

# 2. Check if VS Code can find it
echo "2️⃣ Checking VS Code CLI..."
if command -v code &> /dev/null; then
    VS_CODE_CHECK=$(code --list-extensions | grep -i "$EXTENSION_ID" || echo "not_found")
    if [ "$VS_CODE_CHECK" != "not_found" ]; then
        echo -e "${GREEN}✅ VS Code CLI: INSTALLED${NC}"
    else
        echo -e "${YELLOW}⏳ VS Code CLI: NOT INSTALLED (try: code --install-extension $EXTENSION_ID)${NC}"
    fi
else
    echo -e "${RED}❌ VS Code CLI: NOT AVAILABLE${NC}"
fi

echo ""

# 3. Check if Cursor can find it  
echo "3️⃣ Checking Cursor IDE..."
if command -v cursor &> /dev/null; then
    CURSOR_CHECK=$(cursor --list-extensions | grep -i "$EXTENSION_ID" || echo "not_found")
    if [ "$CURSOR_CHECK" != "not_found" ]; then
        echo -e "${GREEN}✅ Cursor IDE: INSTALLED${NC}"
    else
        # Try to install to test availability
        echo "   🧪 Testing installation availability..."
        CURSOR_INSTALL_TEST=$(cursor --install-extension "$EXTENSION_ID" 2>&1)
        if echo "$CURSOR_INSTALL_TEST" | grep -q "not found"; then
            echo -e "${YELLOW}⏳ Cursor IDE: NOT AVAILABLE YET${NC}"
            echo "   💡 Cursor syncs with VS Code Marketplace every 15-60 minutes"
        elif echo "$CURSOR_INSTALL_TEST" | grep -q "Installing"; then
            echo -e "${GREEN}✅ Cursor IDE: AVAILABLE FOR INSTALLATION${NC}"
        else
            echo -e "${YELLOW}⏳ Cursor IDE: STATUS UNKNOWN${NC}"
        fi
    fi
else
    echo -e "${RED}❌ Cursor CLI: NOT AVAILABLE${NC}"
fi

echo ""

# 4. Download stats (if available)
echo "4️⃣ Checking extension stats..."
STATS_URL="https://marketplace.visualstudio.com/_apis/public/gallery/extensionquery"
echo "   📈 Stats page: https://marketplace.visualstudio.com/manage/publishers/repltile/extensions/repltile-clojure-repl/hub"

echo ""
echo "🔄 To recheck, run: ./check-propagation.sh"
echo "⏰ Typical propagation time: 15-60 minutes after publication"
echo "" 