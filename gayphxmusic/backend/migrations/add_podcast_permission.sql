-- Add podcast_permission column to rights_permissions table
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS podcast_permission BOOLEAN DEFAULT FALSE;
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS podcast_granted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS podcast_revoked_at TIMESTAMP WITH TIME ZONE;

-- Add commercial compensation columns
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS commercial_compensation_rate NUMERIC(10, 2);
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS commercial_compensation_paid NUMERIC(10, 2) DEFAULT 0;
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS commercial_uses_count INTEGER DEFAULT 0;
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS last_commercial_use TIMESTAMP WITH TIME ZONE;

-- Add rights holder information columns
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS rights_holder_name VARCHAR(255);
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS rights_holder_email VARCHAR(255);
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS rights_holder_phone VARCHAR(50);

-- Add legal information columns
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS copyright_year VARCHAR(4);
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS copyright_owner VARCHAR(255);
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS publisher VARCHAR(255);
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS label VARCHAR(255);

-- Add additional terms columns
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS custom_terms TEXT;
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS restrictions TEXT;

-- Add audit columns
ALTER TABLE rights_permissions ADD COLUMN IF NOT EXISTS last_modified_by UUID REFERENCES artists(id);
