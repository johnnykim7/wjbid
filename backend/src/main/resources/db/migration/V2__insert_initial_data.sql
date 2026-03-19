-- ====================================
-- Initial Data (Keyword Groups)
-- ====================================

-- Main keyword: 411th csb
INSERT INTO keyword_groups (id, group_name, group_type, keywords, active, priority)
VALUES (
    UNHEX(REPLACE(UUID(), '-', '')),
    'MAIN',
    'PRIMARY',
    JSON_ARRAY('411th csb', '411th CSB', '411 CSB'),
    TRUE,
    10
);

-- Korea keyword
INSERT INTO keyword_groups (id, group_name, group_type, keywords, active, priority)
VALUES (
    UNHEX(REPLACE(UUID(), '-', '')),
    'KOREA',
    'SECONDARY',
    JSON_ARRAY('korea', 'Korea', 'KOREA', 'South Korea', 'Republic of Korea'),
    TRUE,
    5
);

-- USFK Location keywords
INSERT INTO keyword_groups (id, group_name, group_type, keywords, active, priority)
VALUES (
    UNHEX(REPLACE(UUID(), '-', '')),
    'USFK_LOCATIONS',
    'LOCATION',
    JSON_ARRAY(
        'Camp Casey',
        'Osan AB',
        'Osan Air Base',
        'Kunsan AB',
        'Kunsan Air Base',
        'Camp Humphreys',
        'Humphreys',
        'Camp Henry',
        'USAG Daegu',
        'Camp Walker',
        'Camp Carroll'
    ),
    TRUE,
    3
);
