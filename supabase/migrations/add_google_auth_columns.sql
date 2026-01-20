-- Migration: Add Google OAuth columns to users table
-- Run this in your Supabase SQL Editor

-- Add google_id column (Supabase auth user ID)
ALTER TABLE users
ADD COLUMN IF NOT EXISTS google_id UUID UNIQUE;

-- Add email column
ALTER TABLE users
ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Create index for faster google_id lookups
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);

-- Create index for email
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Make ticket_id optional (since Google users don't need tickets)
ALTER TABLE users
ALTER COLUMN ticket_id DROP NOT NULL;

-- ============================================================
-- IMPORTANT: Configure Google OAuth in Supabase Dashboard
-- ============================================================
-- 1. Go to Supabase Dashboard > Authentication > Providers
-- 2. Enable Google provider
-- 3. Add your Google OAuth credentials:
--    - Client ID: from Google Cloud Console
--    - Client Secret: from Google Cloud Console
-- 4. Add redirect URL to Google Console:
--    https://[your-project-ref].supabase.co/auth/v1/callback
-- ============================================================
