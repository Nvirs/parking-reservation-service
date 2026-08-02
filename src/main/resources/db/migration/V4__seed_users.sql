-- test users covering every EV / handicapped-permit combination
INSERT INTO users (name, is_electric, has_handicapped_permit) VALUES
    ('Alice', FALSE, FALSE),
    ('Bob', TRUE, FALSE),
    ('Carol', FALSE, TRUE),
    ('Dave', TRUE, TRUE);
