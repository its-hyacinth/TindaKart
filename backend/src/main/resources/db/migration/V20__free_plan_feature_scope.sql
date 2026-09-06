DELETE FROM package_features
WHERE package_id = (SELECT id FROM packages WHERE name = 'Free')
  AND feature_key NOT IN ('POS', 'CATALOG');

UPDATE packages
SET description = 'Owner-operated POS and catalog with optional paid staff seats and feature add-ons.'
WHERE name = 'Free';
