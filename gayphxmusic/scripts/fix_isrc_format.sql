-- Fix ISRC format migration script
-- This converts old format (USGPHX250000) to new format (US-GPH-25-00000)

-- First, let's see what we have
SELECT 
    id,
    isrc_code,
    country_code,
    registrant_code,
    year,
    sequence_number
FROM isrcs 
WHERE isrc_code NOT LIKE '%-%-%-%';

-- Update the old format ISRCs to proper format
UPDATE isrcs 
SET isrc_code = CONCAT(
    country_code, 
    '-', 
    registrant_code, 
    '-', 
    RIGHT(year::text, 2), 
    '-', 
    LPAD(sequence_number::text, 5, '0')
)
WHERE isrc_code NOT LIKE '%-%-%-%';

-- Also update the registrant_code to the correct 3-character format
UPDATE isrcs 
SET registrant_code = 'GPH'
WHERE registrant_code = 'GPHX';

-- Verify the changes
SELECT 
    id,
    isrc_code,
    country_code,
    registrant_code,
    year,
    sequence_number
FROM isrcs 
ORDER BY created_at;

