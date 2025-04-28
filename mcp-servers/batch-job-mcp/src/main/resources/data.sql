-- Inserting categories
INSERT INTO categories (category) VALUES 
    ('Automotive'),
    ('Business'),
    ('Culture'),
    ('Education'),
    ('Entertainment and Recreation'),
    ('Facilities'),
    ('Finance'),
    ('Food and Drink'),
    ('Groceries'),
    ('Government'),
    ('Health and Wellness'),
    ('Housing'),
    ('Renting'),
    ('Services'),
    ('Shopping'),
    ('Sports'),
    ('Transportation'),
    ('Salary')
ON CONFLICT (category) DO NOTHING;