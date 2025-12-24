# Data Security Deployment Guide

## Encryption at Rest

### PostgreSQL Encryption

#### Option 1: Transparent Data Encryption (TDE)

PostgreSQL does not natively support TDE, but you can use:

1. **File System Encryption**:
   - Use LUKS (Linux Unified Key Setup) for full disk encryption
   - Encrypt the PostgreSQL data directory
   - Example:
     ```bash
     # Create encrypted volume
     cryptsetup luksFormat /dev/sdb1
     cryptsetup luksOpen /dev/sdb1 postgres_data
     mkfs.ext4 /dev/mapper/postgres_data
     mount /dev/mapper/postgres_data /var/lib/postgresql/data
     ```

2. **PostgreSQL Extensions**:
   - Use `pgcrypto` extension for column-level encryption
   - Encrypt sensitive fields at application level before storage

#### Option 2: Application-Level Encryption

Encrypt sensitive fields before storing in database:

```python
from cryptography.fernet import Fernet
import os

# Generate key (store securely, use environment variable)
encryption_key = os.getenv("ENCRYPTION_KEY")
cipher = Fernet(encryption_key)

# Encrypt before storing
encrypted_data = cipher.encrypt(sensitive_data.encode())

# Decrypt when reading
decrypted_data = cipher.decrypt(encrypted_data).decode()
```

### Encrypted Volume Configuration

#### AWS EBS Encryption

1. **Enable EBS Encryption**:
   ```bash
   # Create encrypted EBS volume
   aws ec2 create-volume \
     --size 100 \
     --volume-type gp3 \
     --encrypted \
     --kms-key-id <your-kms-key-id> \
     --availability-zone us-east-1a
   ```

2. **Docker Compose with Encrypted Volume**:
   ```yaml
   services:
     db:
       volumes:
         - encrypted_postgres_data:/var/lib/postgresql/data
   
   volumes:
     encrypted_postgres_data:
       driver: local
       driver_opts:
         type: nfs
         device: ":/encrypted-storage"
   ```

#### Azure Disk Encryption

1. **Enable Azure Disk Encryption**:
   ```bash
   az vm encryption enable \
     --resource-group myResourceGroup \
     --name myVM \
     --disk-encryption-keyvault myKeyVault \
     --key-encryption-key myKey
   ```

2. **Use Azure Managed Disks with Encryption**:
   - Enable encryption when creating managed disk
   - Use Azure Key Vault for key management

## Application-Level Encryption for Sensitive Fields

### Fields Requiring Encryption

1. **API Keys**: Store encrypted in database
2. **PII (Personally Identifiable Information)**:
   - Email addresses (if required by compliance)
   - Phone numbers
   - Addresses
   - Payment information
3. **Credentials**: 
   - OAuth tokens
   - Third-party API credentials
   - Database connection strings

### Implementation Example

```python
from backend.utils.encryption import encrypt_field, decrypt_field

class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True)
    email_encrypted = Column(String)  # Encrypted email
    
    @property
    def email(self):
        return decrypt_field(self.email_encrypted)
    
    @email.setter
    def email(self, value):
        self.email_encrypted = encrypt_field(value)
```

## Backup Encryption

### Database Backup Encryption

1. **PostgreSQL pg_dump with Encryption**:
   ```bash
   # Create encrypted backup
   pg_dump -U librelog librelog | gpg --encrypt --recipient backup@example.com > backup.sql.gpg
   
   # Restore encrypted backup
   gpg --decrypt backup.sql.gpg | psql -U librelog librelog
   ```

2. **Automated Encrypted Backups**:
   ```bash
   # Backup script with encryption
   #!/bin/bash
   BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
   ENCRYPTION_KEY=$(cat /etc/librelog/backup_key)
   
   pg_dump -U librelog librelog > "$BACKUP_FILE"
   openssl enc -aes-256-cbc -salt -in "$BACKUP_FILE" -out "${BACKUP_FILE}.enc" -pass file:/etc/librelog/backup_key
   rm "$BACKUP_FILE"
   ```

### Cloud Backup Encryption

1. **AWS S3 with Server-Side Encryption**:
   - Enable S3 bucket encryption (SSE-S3 or SSE-KMS)
   - Use client-side encryption before upload for additional security

2. **Azure Blob Storage Encryption**:
   - Enable Azure Storage Service Encryption
   - Use Azure Key Vault for key management

3. **Google Cloud Storage Encryption**:
   - Enable Cloud KMS encryption
   - Use customer-managed encryption keys (CMEK)

## Key Management

### Best Practices

1. **Never Store Keys in Code**:
   - Use environment variables
   - Use secret management services (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault)

2. **Key Rotation**:
   - Rotate encryption keys regularly (every 90 days recommended)
   - Maintain key versioning for decryption of old data

3. **Key Access Control**:
   - Limit access to encryption keys
   - Use IAM roles and policies
   - Audit all key access

### HashiCorp Vault Integration

```python
import hvac

# Initialize Vault client
vault_client = hvac.Client(url='https://vault.example.com:8200')
vault_client.token = os.getenv('VAULT_TOKEN')

# Retrieve encryption key
encryption_key = vault_client.secrets.kv.v2.read_secret_version(
    path='librelog/encryption-key'
)['data']['data']['key']
```

### AWS Secrets Manager Integration

```python
import boto3

# Initialize Secrets Manager client
secrets_client = boto3.client('secretsmanager', region_name='us-east-1')

# Retrieve encryption key
response = secrets_client.get_secret_value(SecretId='librelog/encryption-key')
encryption_key = response['SecretString']
```

## Compliance Requirements

### PCI-DSS

- Encrypt cardholder data at rest
- Use strong encryption (AES-256 minimum)
- Secure key management
- Regular key rotation

### HIPAA

- Encrypt PHI (Protected Health Information) at rest
- Use NIST-approved encryption algorithms
- Secure key management
- Access controls on encrypted data

### GDPR

- Encrypt personal data at rest
- Implement data protection by design
- Regular security assessments
- Breach notification procedures

## Monitoring and Auditing

- Monitor encryption key access
- Audit encryption/decryption operations
- Alert on encryption failures
- Regular security assessments
- Penetration testing of encryption implementation

## References

- [NIST SP 800-111: Guide to Storage Encryption Technologies](https://csrc.nist.gov/publications/detail/sp/800-111/final)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [PCI-DSS Requirement 3: Protect Stored Cardholder Data](https://www.pcisecuritystandards.org/)


