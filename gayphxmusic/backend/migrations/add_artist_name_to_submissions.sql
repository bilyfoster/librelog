-- Add artist_name column to submissions table
ALTER TABLE submissions ADD COLUMN artist_name VARCHAR(255) NOT NULL DEFAULT '';

-- Remove unique constraint from artists.email
ALTER TABLE artists DROP CONSTRAINT IF EXISTS artists_email_key;

-- Add unique constraint on email + name combination
ALTER TABLE artists ADD CONSTRAINT unique_email_artist_name UNIQUE (email, name);

-- Update existing submissions to have artist_name populated
UPDATE submissions 
SET artist_name = artists.name 
FROM artists 
WHERE submissions.artist_id = artists.id;

-- Make artist_name NOT NULL (remove default after populating)
ALTER TABLE submissions ALTER COLUMN artist_name DROP DEFAULT;

