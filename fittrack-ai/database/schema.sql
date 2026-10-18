-- =============================================================================
-- FITTRACK AI — Production Database Schema (PostgreSQL / Supabase)
-- Robust Schema with Row Level Security (RLS), Foreign Keys, Indexes & Triggers
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Clean existing if re-running
DROP TABLE IF EXISTS audit_events CASCADE;
DROP TABLE IF EXISTS subscription_entitlements CASCADE;
DROP TABLE IF EXISTS notification_preferences CASCADE;
DROP TABLE IF EXISTS sync_operations CASCADE;
DROP TABLE IF EXISTS user_corrections CASCADE;
DROP TABLE IF EXISTS weight_logs CASCADE;
DROP TABLE IF EXISTS water_logs CASCADE;
DROP TABLE IF EXISTS recipe_ingredients CASCADE;
DROP TABLE IF EXISTS recipes CASCADE;
DROP TABLE IF EXISTS meal_items CASCADE;
DROP TABLE IF EXISTS meal_entries CASCADE;
DROP TABLE IF EXISTS recognition_results CASCADE;
DROP TABLE IF EXISTS food_scans CASCADE;
DROP TABLE IF EXISTS custom_foods CASCADE;
DROP TABLE IF EXISTS food_nutrients CASCADE;
DROP TABLE IF EXISTS nutrient_definitions CASCADE;
DROP TABLE IF EXISTS food_catalog CASCADE;
DROP TABLE IF EXISTS nutrition_goals CASCADE;
DROP TABLE IF EXISTS user_preferences CASCADE;
DROP TABLE IF EXISTS profiles CASCADE;

-- -----------------------------------------------------------------------------
-- 1. PROFILES (extends Supabase auth.users or standalone auth)
-- -----------------------------------------------------------------------------
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    avatar_url TEXT,
    gender VARCHAR(32) DEFAULT 'not_specified',
    date_of_birth DATE,
    height_cm NUMERIC(5,2) DEFAULT 170.0,
    current_weight_kg NUMERIC(5,2) DEFAULT 70.0,
    target_weight_kg NUMERIC(5,2) DEFAULT 68.0,
    activity_level VARCHAR(32) DEFAULT 'moderately_active', -- sedentary, lightly_active, moderately_active, very_active, extra_active
    dietary_lifestyle VARCHAR(64) DEFAULT 'balanced', -- balanced, vegetarian, vegan, keto, high_protein, low_carb
    health_connect_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_profiles_user_id ON profiles(user_id);

-- -----------------------------------------------------------------------------
-- 2. USER PREFERENCES
-- -----------------------------------------------------------------------------
CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    preferred_cuisine VARCHAR(64) DEFAULT 'Indian',
    preferred_units VARCHAR(16) DEFAULT 'metric', -- metric (kg, g, ml), imperial (lbs, oz, fl_oz)
    calorie_rollover BOOLEAN DEFAULT FALSE,
    exercise_calorie_adjustment BOOLEAN DEFAULT TRUE,
    dark_mode BOOLEAN DEFAULT TRUE,
    haptic_feedback BOOLEAN DEFAULT TRUE,
    sound_effects BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_user_preferences_user UNIQUE (user_id)
);

-- -----------------------------------------------------------------------------
-- 3. NUTRITION GOALS
-- -----------------------------------------------------------------------------
CREATE TABLE nutrition_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    daily_calories INTEGER NOT NULL DEFAULT 2000,
    protein_g NUMERIC(6,2) NOT NULL DEFAULT 120.0,
    carbs_g NUMERIC(6,2) NOT NULL DEFAULT 220.0,
    fat_g NUMERIC(6,2) NOT NULL DEFAULT 65.0,
    fiber_g NUMERIC(6,2) DEFAULT 30.0,
    sugar_limit_g NUMERIC(6,2) DEFAULT 35.0,
    sodium_limit_mg NUMERIC(6,2) DEFAULT 2300.0,
    water_goal_ml INTEGER DEFAULT 2500,
    is_active BOOLEAN DEFAULT TRUE,
    start_date DATE DEFAULT CURRENT_DATE,
    end_date DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_user_active_goal UNIQUE (user_id, is_active, start_date)
);

CREATE INDEX idx_nutrition_goals_user ON nutrition_goals(user_id, is_active);

-- -----------------------------------------------------------------------------
-- 4. FOOD CATALOG (Global verified database: USDA + Regional / Indian)
-- -----------------------------------------------------------------------------
CREATE TABLE food_catalog (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(128) NOT NULL,
    cuisine VARCHAR(64) DEFAULT 'Global',
    default_serving_unit VARCHAR(64) DEFAULT 'serving',
    default_serving_weight_g NUMERIC(6,2) DEFAULT 100.0,
    density_g_per_ml NUMERIC(4,2) DEFAULT 1.0,
    ingredients JSONB DEFAULT '[]'::jsonb,
    aliases JSONB DEFAULT '[]'::jsonb,
    verified BOOLEAN DEFAULT TRUE,
    source_attribution VARCHAR(255) DEFAULT 'USDA / IFCT',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_food_catalog_name ON food_catalog(name);
CREATE INDEX idx_food_catalog_category ON food_catalog(category);
CREATE INDEX idx_food_catalog_aliases ON food_catalog USING gin(aliases);

-- -----------------------------------------------------------------------------
-- 5. NUTRIENT DEFINITIONS (Master dictionary of vitamins, minerals & macros)
-- -----------------------------------------------------------------------------
CREATE TABLE nutrient_definitions (
    id VARCHAR(32) PRIMARY KEY, -- calories, protein, carbs, fat, fiber, sugar, sodium, etc.
    name VARCHAR(128) NOT NULL,
    unit VARCHAR(16) NOT NULL, -- kcal, g, mg, mcg, IU
    is_macro BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 100
);

-- -----------------------------------------------------------------------------
-- 6. FOOD NUTRIENTS (Per 100g normalized values)
-- -----------------------------------------------------------------------------
CREATE TABLE food_nutrients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    food_id VARCHAR(64) NOT NULL REFERENCES food_catalog(id) ON DELETE CASCADE,
    calories NUMERIC(8,2) NOT NULL DEFAULT 0,
    protein_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    carbs_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    fat_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    saturated_fat_g NUMERIC(8,2) DEFAULT 0,
    fiber_g NUMERIC(8,2) DEFAULT 0,
    sugar_g NUMERIC(8,2) DEFAULT 0,
    sodium_mg NUMERIC(8,2) DEFAULT 0,
    cholesterol_mg NUMERIC(8,2) DEFAULT 0,
    potassium_mg NUMERIC(8,2) DEFAULT 0,
    calcium_mg NUMERIC(8,2) DEFAULT 0,
    iron_mg NUMERIC(8,2) DEFAULT 0,
    vitamin_c_mg NUMERIC(8,2) DEFAULT 0,
    vitamin_d_mcg NUMERIC(8,2) DEFAULT 0,
    micronutrients JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT uq_food_nutrients_food UNIQUE (food_id)
);

CREATE INDEX idx_food_nutrients_food_id ON food_nutrients(food_id);

-- -----------------------------------------------------------------------------
-- 7. CUSTOM FOODS (User-created or verified custom items)
-- -----------------------------------------------------------------------------
CREATE TABLE custom_foods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255),
    barcode VARCHAR(64),
    serving_unit VARCHAR(64) DEFAULT 'portion',
    serving_weight_g NUMERIC(6,2) DEFAULT 100.0,
    calories NUMERIC(8,2) NOT NULL,
    protein_g NUMERIC(8,2) NOT NULL,
    carbs_g NUMERIC(8,2) NOT NULL,
    fat_g NUMERIC(8,2) NOT NULL,
    fiber_g NUMERIC(8,2) DEFAULT 0,
    sugar_g NUMERIC(8,2) DEFAULT 0,
    sodium_mg NUMERIC(8,2) DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_custom_foods_user ON custom_foods(user_id);
CREATE INDEX idx_custom_foods_barcode ON custom_foods(barcode);

-- -----------------------------------------------------------------------------
-- 8. FOOD SCANS (Captured or uploaded photo records)
-- -----------------------------------------------------------------------------
CREATE TABLE food_scans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    image_storage_path TEXT NOT NULL,
    image_hash VARCHAR(64),
    image_width INTEGER,
    image_height INTEGER,
    status VARCHAR(32) DEFAULT 'pending', -- pending, processing, completed, failed
    model_provider VARCHAR(64) DEFAULT 'gemini-vision',
    model_version VARCHAR(64) DEFAULT '1.5-flash',
    processing_latency_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_food_scans_user ON food_scans(user_id);
CREATE INDEX idx_food_scans_created ON food_scans(created_at DESC);

-- -----------------------------------------------------------------------------
-- 9. RECOGNITION RESULTS (AI Model raw outputs before user confirmation)
-- -----------------------------------------------------------------------------
CREATE TABLE recognition_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id UUID NOT NULL REFERENCES food_scans(id) ON DELETE CASCADE,
    raw_response JSONB NOT NULL,
    detected_items JSONB NOT NULL, -- Array of candidate items with bounding boxes & confidences
    overall_confidence NUMERIC(4,3) NOT NULL DEFAULT 0.85,
    needs_user_clarification BOOLEAN DEFAULT FALSE,
    clarification_question TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_recognition_results_scan ON recognition_results(scan_id);

-- -----------------------------------------------------------------------------
-- 10. MEAL ENTRIES (Parent meal logged by the user)
-- -----------------------------------------------------------------------------
CREATE TABLE meal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    scan_id UUID REFERENCES food_scans(id) ON DELETE SET NULL,
    meal_type VARCHAR(32) NOT NULL, -- breakfast, lunch, dinner, snack
    meal_title VARCHAR(255) NOT NULL,
    logged_date DATE NOT NULL DEFAULT CURRENT_DATE,
    logged_time TIME NOT NULL DEFAULT CURRENT_TIME,
    logged_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    total_calories NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_protein_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_carbs_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_fat_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_fiber_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_sugar_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_sodium_mg NUMERIC(8,2) NOT NULL DEFAULT 0,
    notes TEXT,
    photo_url TEXT,
    source VARCHAR(32) DEFAULT 'ai_scan', -- ai_scan, barcode, voice, manual, recipe
    is_favorite BOOLEAN DEFAULT FALSE,
    sync_status VARCHAR(16) DEFAULT 'synced', -- synced, pending_sync, conflict
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_meal_entries_user_date ON meal_entries(user_id, logged_date);
CREATE INDEX idx_meal_entries_timestamp ON meal_entries(logged_timestamp DESC);

-- -----------------------------------------------------------------------------
-- 11. MEAL ITEMS (Individual items/ingredients belonging to a meal)
-- -----------------------------------------------------------------------------
CREATE TABLE meal_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_id UUID NOT NULL REFERENCES meal_entries(id) ON DELETE CASCADE,
    food_id VARCHAR(64) REFERENCES food_catalog(id) ON DELETE SET NULL,
    custom_food_id UUID REFERENCES custom_foods(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    serving_unit VARCHAR(64) NOT NULL DEFAULT 'g',
    serving_weight_g NUMERIC(8,2) NOT NULL DEFAULT 100.0,
    quantity NUMERIC(6,2) NOT NULL DEFAULT 1.0,
    calories NUMERIC(8,2) NOT NULL,
    protein_g NUMERIC(8,2) NOT NULL,
    carbs_g NUMERIC(8,2) NOT NULL,
    fat_g NUMERIC(8,2) NOT NULL,
    fiber_g NUMERIC(8,2) DEFAULT 0,
    sugar_g NUMERIC(8,2) DEFAULT 0,
    sodium_mg NUMERIC(8,2) DEFAULT 0,
    cooking_method VARCHAR(64) DEFAULT 'standard', -- raw, boiled, fried, grilled, roasted, curried
    oil_adjustment VARCHAR(32) DEFAULT 'normal', -- none, low, normal, high
    confidence NUMERIC(4,3) DEFAULT 1.0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_meal_items_meal ON meal_items(meal_id);

-- -----------------------------------------------------------------------------
-- 12. RECIPES (Saved custom recipes)
-- -----------------------------------------------------------------------------
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    servings INTEGER DEFAULT 1,
    instructions TEXT,
    total_calories NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_protein_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_carbs_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_fat_g NUMERIC(8,2) NOT NULL DEFAULT 0,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_recipes_user ON recipes(user_id);

-- -----------------------------------------------------------------------------
-- 13. RECIPE INGREDIENTS
-- -----------------------------------------------------------------------------
CREATE TABLE recipe_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    food_id VARCHAR(64) REFERENCES food_catalog(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    weight_g NUMERIC(8,2) NOT NULL,
    unit VARCHAR(64) DEFAULT 'g',
    calories NUMERIC(8,2) NOT NULL,
    protein_g NUMERIC(8,2) NOT NULL,
    carbs_g NUMERIC(8,2) NOT NULL,
    fat_g NUMERIC(8,2) NOT NULL
);

CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients(recipe_id);

-- -----------------------------------------------------------------------------
-- 14. WATER LOGS
-- -----------------------------------------------------------------------------
CREATE TABLE water_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    logged_date DATE NOT NULL DEFAULT CURRENT_DATE,
    amount_ml INTEGER NOT NULL DEFAULT 250,
    logged_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_water_logs_user_date ON water_logs(user_id, logged_date);

-- -----------------------------------------------------------------------------
-- 15. WEIGHT LOGS
-- -----------------------------------------------------------------------------
CREATE TABLE weight_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    logged_date DATE NOT NULL DEFAULT CURRENT_DATE,
    weight_kg NUMERIC(5,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_user_weight_date UNIQUE (user_id, logged_date)
);

CREATE INDEX idx_weight_logs_user ON weight_logs(user_id, logged_date DESC);

-- -----------------------------------------------------------------------------
-- 16. USER CORRECTIONS (Fine-tuning feedback loop for AI model improvements)
-- -----------------------------------------------------------------------------
CREATE TABLE user_corrections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    scan_id UUID REFERENCES food_scans(id) ON DELETE SET NULL,
    predicted_item_name VARCHAR(255) NOT NULL,
    corrected_item_name VARCHAR(255) NOT NULL,
    predicted_weight_g NUMERIC(6,2),
    corrected_weight_g NUMERIC(6,2),
    correction_reason VARCHAR(128),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_user_corrections_user ON user_corrections(user_id);

-- -----------------------------------------------------------------------------
-- 17. SYNC OPERATIONS (Idempotent batch offline syncing)
-- -----------------------------------------------------------------------------
CREATE TABLE sync_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    client_operation_id VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64) NOT NULL, -- meal_entry, water_log, weight_log, goal
    operation VARCHAR(16) NOT NULL, -- INSERT, UPDATE, DELETE
    payload JSONB NOT NULL,
    status VARCHAR(16) DEFAULT 'processed',
    processed_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_client_op UNIQUE (user_id, client_operation_id)
);

CREATE INDEX idx_sync_ops_user ON sync_operations(user_id, client_operation_id);

-- -----------------------------------------------------------------------------
-- 18. NOTIFICATION PREFERENCES
-- -----------------------------------------------------------------------------
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    meal_reminders_enabled BOOLEAN DEFAULT TRUE,
    breakfast_reminder_time TIME DEFAULT '08:30:00',
    lunch_reminder_time TIME DEFAULT '13:00:00',
    dinner_reminder_time TIME DEFAULT '20:00:00',
    water_reminders_enabled BOOLEAN DEFAULT TRUE,
    water_interval_hours INTEGER DEFAULT 2,
    weekly_insight_notification BOOLEAN DEFAULT TRUE,
    fcm_token TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_notif_pref_user UNIQUE (user_id)
);

-- -----------------------------------------------------------------------------
-- 19. SUBSCRIPTION ENTITLEMENTS (Free vs Pro tiers)
-- -----------------------------------------------------------------------------
CREATE TABLE subscription_entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    tier VARCHAR(32) DEFAULT 'pro', -- free, pro, premium_annual
    status VARCHAR(32) DEFAULT 'active', -- active, expired, canceled, trial
    scans_remaining_today INTEGER DEFAULT 100,
    valid_until TIMESTAMPTZ DEFAULT NOW() + INTERVAL '1 year',
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_sub_user UNIQUE (user_id)
);

-- -----------------------------------------------------------------------------
-- 20. AUDIT EVENTS (Security and compliance logging)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(user_id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL, -- login, logout, password_change, account_deletion, data_export
    ip_address VARCHAR(64),
    user_agent TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_events_user ON audit_events(user_id, created_at DESC);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Strict user boundary isolation
-- =============================================================================

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE nutrition_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE custom_foods ENABLE ROW LEVEL SECURITY;
ALTER TABLE food_scans ENABLE ROW LEVEL SECURITY;
ALTER TABLE recognition_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipe_ingredients ENABLE ROW LEVEL SECURITY;
ALTER TABLE water_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE weight_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_corrections ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_entitlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;

-- Users may only view and update their own profile
CREATE POLICY profiles_owner_policy ON profiles
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY user_pref_owner_policy ON user_preferences
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY goals_owner_policy ON nutrition_goals
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY custom_foods_owner_policy ON custom_foods
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY scans_owner_policy ON food_scans
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY meals_owner_policy ON meal_entries
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY water_owner_policy ON water_logs
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY weight_owner_policy ON weight_logs
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY sync_owner_policy ON sync_operations
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY recipes_owner_policy ON recipes
    FOR ALL USING (auth.uid() = user_id OR is_public = TRUE);

-- Public read for food_catalog & nutrient_definitions
ALTER TABLE food_catalog ENABLE ROW LEVEL SECURITY;
CREATE POLICY food_catalog_public_read ON food_catalog FOR SELECT USING (true);

ALTER TABLE nutrient_definitions ENABLE ROW LEVEL SECURITY;
CREATE POLICY nutrient_def_public_read ON nutrient_definitions FOR SELECT USING (true);

ALTER TABLE food_nutrients ENABLE ROW LEVEL SECURITY;
CREATE POLICY food_nutrients_public_read ON food_nutrients FOR SELECT USING (true);
