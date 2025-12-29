# Build Requirements

## Java Development Kit (JDK)

This project requires **Java 21 JDK** (not just JRE) for compilation.

### Installation

```bash
sudo apt-get update
sudo apt-get install openjdk-21-jdk-headless
```

### Verification

After installation, verify javac is available:
```bash
javac -version
```

You should see output like:
```
javac 21.0.9
```

### Build Command

Once JDK is installed, build the project:
```bash
mvn clean install
```

Or compile only:
```bash
mvn clean compile
```
