const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const https = require('https');

async function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, (response) => {
      response.pipe(file);
      file.on('finish', () => {
        file.close();
        resolve();
      });
    }).on('error', (err) => {
      fs.unlinkSync(dest);
      reject(err);
    });
  });
}

async function bundleDependencies() {
  const libDir = path.join(__dirname, 'lib');

  console.log('📦 Bundling REPLTILE dependencies...');

  // Criar pasta lib se não existir
  if (!fs.existsSync(libDir)) {
    fs.mkdirSync(libDir);
    console.log('✅ Created lib directory');
  }

  // Definir dependências e suas URLs (Clojars para nrepl/cider, Maven Central para outros)
  const dependencies = [
    {
      name: 'nrepl-1.3.1.jar',
      url: 'https://repo.clojars.org/nrepl/nrepl/1.3.1/nrepl-1.3.1.jar'
    },
    {
      name: 'cider-nrepl-0.57.0.jar',
      url: 'https://repo.clojars.org/cider/cider-nrepl/0.57.0/cider-nrepl-0.57.0.jar'
    },
    {
      name: 'orchard-0.36.0.jar',
      url: 'https://repo.clojars.org/cider/orchard/0.36.0/orchard-0.36.0.jar'
    },
    {
      name: 'tools.namespace-1.4.4.jar',
      url: 'https://repo1.maven.org/maven2/org/clojure/tools.namespace/1.4.4/tools.namespace-1.4.4.jar'
    },
    {
      name: 'clojure-1.11.1.jar',
      url: 'https://repo1.maven.org/maven2/org/clojure/clojure/1.11.1/clojure-1.11.1.jar'
    },
    {
      name: 'spec.alpha-0.3.218.jar',
      url: 'https://repo1.maven.org/maven2/org/clojure/spec.alpha/0.3.218/spec.alpha-0.3.218.jar'
    },
    {
      name: 'core.specs.alpha-0.2.62.jar',
      url: 'https://repo1.maven.org/maven2/org/clojure/core.specs.alpha/0.2.62/core.specs.alpha-0.2.62.jar'
    }
  ];

  // Baixar cada dependência
  for (const dep of dependencies) {
    const destPath = path.join(libDir, dep.name);

    // Pular se já existe
    if (fs.existsSync(destPath)) {
      console.log(`⏭️  Skipping ${dep.name} (already exists)`);
      continue;
    }

    console.log(`📥 Downloading ${dep.name}...`);
    try {
      await downloadFile(dep.url, destPath);
      console.log(`✅ Downloaded ${dep.name}`);
    } catch (error) {
      console.error(`❌ Failed to download ${dep.name}:`, error.message);
      process.exit(1);
    }
  }

  // Verificar se todos os arquivos foram baixados
  console.log('\n📋 Dependency Summary:');
  let totalSize = 0;
  for (const dep of dependencies) {
    const filePath = path.join(libDir, dep.name);
    if (fs.existsSync(filePath)) {
      const stats = fs.statSync(filePath);
      const sizeKB = Math.round(stats.size / 1024);
      totalSize += stats.size;
      console.log(`✅ ${dep.name} (${sizeKB} KB)`);
    } else {
      console.log(`❌ ${dep.name} (MISSING)`);
    }
  }

  console.log(`\n🎉 Bundle complete! Total size: ${Math.round(totalSize / 1024)} KB`);
  console.log(`📁 Dependencies bundled in: ${libDir}`);
}

if (require.main === module) {
  bundleDependencies().catch(console.error);
}

module.exports = { bundleDependencies }; 