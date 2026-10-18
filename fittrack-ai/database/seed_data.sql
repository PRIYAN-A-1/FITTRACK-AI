-- =============================================================================
-- FITTRACK AI — Database Seed Data
-- =============================================================================

INSERT INTO nutrient_definitions (id, name, unit, is_macro, display_order) VALUES
('calories', 'Energy', 'kcal', true, 1),
('protein', 'Protein', 'g', true, 2),
('carbs', 'Carbohydrates', 'g', true, 3),
('fat', 'Total Fat', 'g', true, 4),
('fiber', 'Dietary Fiber', 'g', false, 5),
('sugar', 'Total Sugars', 'g', false, 6),
('sodium', 'Sodium', 'mg', false, 7),
('saturated_fat', 'Saturated Fat', 'g', false, 8),
('cholesterol', 'Cholesterol', 'mg', false, 9)
ON CONFLICT (id) DO NOTHING;

-- Food Catalog
INSERT INTO food_catalog (id, name, category, cuisine, default_serving_unit, default_serving_weight_g, density_g_per_ml, ingredients, aliases, verified, source_attribution) VALUES
('ind_001', 'Egg Fried Rice', 'Rice Dishes', 'Indian / Indo-Chinese', 'plate', 300, 0.75, '["cooked rice", "eggs", "spring onion", "carrots", "garlic", "soy sauce", "vegetable oil", "black pepper"]', '["egg rice", "muttai saadham", "anda fried rice", "egg pulao"]', true, 'IFCT / USDA Composite'),
('ind_002', 'Idli', 'Breakfast', 'South Indian', 'piece', 60, 0.65, '["fermented parboiled rice", "urad dal", "fenugreek seeds", "salt"]', '["steamed rice cake", "idly"]', true, 'IFCT (Indian Food Composition Tables)'),
('ind_003', 'Plain Dosa', 'Breakfast', 'South Indian', 'piece', 85, 0.55, '["rice and urad dal batter", "ghee/oil", "salt"]', '["dosai", "plain roast", "ghee roast"]', true, 'IFCT'),
('ind_004', 'Sambar', 'Lentils & Curries', 'South Indian', 'bowl', 150, 1.02, '["toor dal", "shallots", "drumstick", "tomato", "tamarind", "sambar masala", "mustard seeds", "curry leaves"]', '["sambhar", "paruppu sambar"]', true, 'IFCT'),
('ind_005', 'Coconut Chutney', 'Condiments', 'South Indian', 'tablespoon', 30, 0.95, '["fresh grated coconut", "roasted chana dal", "green chillies", "ginger", "mustard seeds", "oil"]', '["thengai chutney", "white chutney"]', true, 'IFCT'),
('ind_006', 'Chicken Biryani', 'Rice Dishes', 'Indian', 'portion', 350, 0.82, '["basmati rice", "chicken thigh", "onion", "yogurt", "ghee", "biryani spices", "mint", "coriander"]', '["murgh biryani", "dum biryani", "hyderabadi chicken biryani"]', true, 'IFCT'),
('ind_007', 'Chapati / Roti', 'Breads', 'Indian', 'piece', 45, 0.60, '["whole wheat atta", "water", "salt", "light oil brushing"]', '["roti", "phulka", "wheat flatbread"]', true, 'IFCT'),
('ind_008', 'Paneer Butter Masala', 'Curries', 'North Indian', 'bowl', 200, 1.05, '["paneer cottage cheese", "tomato puree", "butter", "cream", "cashew paste", "garam masala", "kasuri methi"]', '["paneer makhani", "butter paneer"]', true, 'IFCT'),
('ind_009', 'Steamed White Rice', 'Grains', 'Global / Indian', 'cup', 160, 0.80, '["white rice", "water"]', '["sadam", "boiled rice", "cooked rice", "chawal"]', true, 'USDA FoodData Central / IFCT'),
('ind_010', 'Potato Fry (Aloo Fry)', 'Sides & Vegetables', 'Indian', 'serving', 100, 0.70, '["potatoes", "vegetable oil", "mustard seeds", "red chilli powder", "turmeric", "salt"]', '["urulaikizhangu varuval", "aloo roast", "potato poriyal"]', true, 'IFCT'),
('ind_011', 'Curd (Plain Dahi / Yogurt)', 'Dairy', 'Indian', 'katori / cup', 120, 1.03, '["pasteurized whole milk", "live active probiotic cultures"]', '["thayir", "dahi", "plain yogurt"]', true, 'IFCT'),
('ind_012', 'Rasam', 'Soups & Broths', 'South Indian', 'cup', 150, 1.01, '["tamarind water", "tomatoes", "garlic", "crushed black pepper", "cumin", "coriander", "mustard seeds", "curry leaves"]', '["saaru", "tomato rasam", "pepper rasam"]', true, 'IFCT'),
('ind_013', 'Dal Tadka', 'Lentils & Curries', 'Indian', 'bowl', 180, 1.04, '["yellow toor dal", "onion", "tomato", "ghee", "cumin seeds", "garlic", "green chillies"]', '["yellow dal", "paruppu", "tadka dal"]', true, 'IFCT'),
('usd_001', 'Grilled Chicken Breast', 'Poultry & Meat', 'Global', 'fillet', 150, 0.85, '["boneless skinless chicken breast", "olive oil seasoning", "salt", "black pepper"]', '["chicken breast", "grilled chicken", "baked chicken"]', true, 'USDA FoodData Central #171077'),
('usd_002', 'Boiled Egg', 'Eggs & Dairy', 'Global', 'egg (large)', 50, 0.98, '["whole chicken egg"]', '["hard boiled egg", "boiled egg", "egg"]', true, 'USDA FoodData Central #171287'),
('usd_003', 'Rolled Oatmeal (Cooked)', 'Breakfast', 'Global', 'bowl', 240, 0.95, '["rolled oats", "water", "pinch salt"]', '["oats porridge", "oatmeal"]', true, 'USDA FoodData Central'),
('usd_004', 'Fresh Banana', 'Fruits', 'Global', 'medium banana', 118, 0.92, '["fresh raw banana"]', '["banana", "kele", "vazhaipazham"]', true, 'USDA FoodData Central #173944'),
('usd_005', 'Avocado', 'Fruits & Healthy Fats', 'Global', 'half avocado', 80, 0.90, '["fresh Hass avocado"]', '["butter fruit", "avocado"]', true, 'USDA FoodData Central #171705'),
('usd_006', 'Cheeseburger', 'Fast Food', 'American', 'burger', 220, 0.65, '["burger bun", "beef patty", "cheddar cheese", "lettuce", "tomato", "sauce"]', '["burger", "beef burger", "cheeseburger"]', true, 'USDA FoodData Central'),
('usd_007', 'French Fries', 'Fast Food', 'Global', 'medium portion', 117, 0.50, '["potatoes", "vegetable oil", "salt"]', '["fries", "chips", "french fries"]', true, 'USDA FoodData Central')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, category = EXCLUDED.category;

-- Nutrients per 100g
INSERT INTO food_nutrients (food_id, calories, protein_g, carbs_g, fat_g, fiber_g, sugar_g, sodium_mg, saturated_fat_g, cholesterol_mg) VALUES
('ind_001', 178, 6.8, 24.2, 5.9, 1.2, 0.8, 380, 1.1, 85),
('ind_002', 136, 4.2, 28.5, 0.4, 1.5, 0.2, 180, 0.1, 0),
('ind_003', 210, 5.1, 34.2, 6.2, 2.1, 0.5, 240, 1.8, 4),
('ind_004', 68, 3.4, 9.8, 1.8, 2.6, 1.9, 310, 0.3, 0),
('ind_005', 230, 3.8, 7.4, 21.0, 4.5, 2.1, 280, 18.0, 0),
('ind_006', 215, 9.8, 24.5, 8.5, 1.4, 1.2, 420, 2.4, 48),
('ind_007', 265, 8.9, 52.0, 3.2, 6.8, 1.0, 190, 0.6, 0),
('ind_008', 220, 7.5, 8.2, 18.1, 1.8, 3.4, 360, 8.9, 38),
('ind_009', 130, 2.7, 28.2, 0.3, 0.4, 0.1, 1, 0.1, 0),
('ind_010', 185, 2.4, 22.0, 9.8, 2.8, 1.1, 320, 1.4, 0),
('ind_011', 61, 3.5, 4.7, 3.3, 0.0, 4.7, 46, 2.1, 13),
('ind_012', 38, 1.2, 6.1, 1.0, 1.1, 1.5, 290, 0.2, 0),
('ind_013', 115, 6.2, 14.8, 3.6, 4.2, 1.1, 280, 1.2, 4),
('usd_001', 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74, 1.0, 85),
('usd_002', 155, 12.6, 1.1, 10.6, 0.0, 1.1, 124, 3.3, 373),
('usd_003', 71, 2.5, 12.0, 1.5, 1.7, 0.3, 49, 0.3, 0),
('usd_004', 89, 1.1, 22.8, 0.3, 2.6, 12.2, 1, 0.1, 0),
('usd_005', 160, 2.0, 8.5, 14.7, 6.7, 0.7, 7, 2.1, 0),
('usd_006', 258, 14.2, 21.5, 13.0, 1.3, 4.2, 480, 5.4, 42),
('usd_007', 312, 3.4, 41.4, 15.0, 3.8, 0.3, 210, 2.3, 0)
ON CONFLICT (food_id) DO UPDATE SET
calories = EXCLUDED.calories,
protein_g = EXCLUDED.protein_g,
carbs_g = EXCLUDED.carbs_g,
fat_g = EXCLUDED.fat_g;
