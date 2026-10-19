-- Initial Mumbai Metro network: Lines 1, 2A and 7.
-- Station names and order were checked against the operators' own websites (see docs/data-sources.md).
-- Travel and transfer minutes are PROTOTYPE ESTIMATES, not official timetable data.
-- Coordinates are left empty because they were not verified. "area" is an approximate locality for browsing.
-- Flyway runs this file once; ON CONFLICT clauses keep it safe if it is ever re-applied manually.

INSERT INTO metro_line (line_code, name, color, text_color, operator_name, status) VALUES
    ('1',  'Line 1 · Versova – Andheri – Ghatkopar', '#0072BC', '#FFFFFF', 'Mumbai Metro One Pvt. Ltd. (MMOPL)', 'OPERATIONAL'),
    ('2A', 'Line 2A · Dahisar (East) – Andheri (West)', '#FFC600', '#2B2200', 'Maha Mumbai Metro Operation Corporation Ltd. (MMMOCL)', 'OPERATIONAL'),
    ('7',  'Line 7 · Gundavali – Dahisar (East)', '#DA291C', '#FFFFFF', 'Maha Mumbai Metro Operation Corporation Ltd. (MMMOCL)', 'OPERATIONAL')
ON CONFLICT (line_code) DO NOTHING;

INSERT INTO station (code, name, area, status) VALUES
    -- Line 1
    ('VERSOVA',          'Versova',                  'Versova',          'OPEN'),
    ('DN_NAGAR',         'D N Nagar',                'Andheri West',     'OPEN'),
    ('AZAD_NAGAR',       'Azad Nagar',               'Andheri West',     'OPEN'),
    ('ANDHERI',          'Andheri',                  'Andheri',          'OPEN'),
    ('WEH',              'Western Express Highway',  'Andheri East',     'OPEN'),
    ('CHAKALA',          'Chakala',                  'Andheri East',     'OPEN'),
    ('AIRPORT_ROAD',     'Airport Road',             'Andheri East',     'OPEN'),
    ('MAROL_NAKA',       'Marol Naka',               'Andheri East',     'OPEN'),
    ('SAKI_NAKA',        'Saki Naka',                'Saki Naka',        'OPEN'),
    ('ASALPHA',          'Asalpha',                  'Ghatkopar West',   'OPEN'),
    ('JAGRUTI_NAGAR',    'Jagruti Nagar',            'Ghatkopar West',   'OPEN'),
    ('GHATKOPAR',        'Ghatkopar',                'Ghatkopar',        'OPEN'),
    -- Line 2A
    ('DAHISAR_EAST',     'Dahisar (East)',           'Dahisar',          'OPEN'),
    ('ANAND_NAGAR',      'Anand Nagar',              'Dahisar',          'OPEN'),
    ('KANDARPADA',       'Kandarpada',               'Dahisar West',     'OPEN'),
    ('MANDAPESHWAR',     'Mandapeshwar – I.C. Colony', 'Borivali West',  'OPEN'),
    ('EKSAR',            'Eksar',                    'Borivali West',    'OPEN'),
    ('BORIVALI_WEST',    'Borivali (West)',          'Borivali West',    'OPEN'),
    ('SHIMPOLI',         'Shimpoli',                 'Borivali West',    'OPEN'),
    ('KANDIVALI_WEST',   'Kandivali (West)',         'Kandivali West',   'OPEN'),
    ('DAHANUKARWADI',    'Dahanukarwadi',            'Kandivali West',   'OPEN'),
    ('VALNAI',           'Valnai – Meeth Chowki',    'Malad West',       'OPEN'),
    ('MALAD_WEST',       'Malad (West)',             'Malad West',       'OPEN'),
    ('LOWER_MALAD',      'Lower Malad',              'Malad West',       'OPEN'),
    ('BANGUR_NAGAR',     'Bangur Nagar',             'Goregaon West',    'OPEN'),
    ('GOREGAON_WEST',    'Goregaon (West)',          'Goregaon West',    'OPEN'),
    ('OSHIWARA',         'Oshiwara',                 'Oshiwara',         'OPEN'),
    ('LOWER_OSHIWARA',   'Lower Oshiwara',           'Andheri West',     'OPEN'),
    ('ANDHERI_WEST',     'Andheri (West)',           'Andheri West',     'OPEN'),
    -- Line 7 (Dahisar (East) is shared with Line 2A)
    ('GUNDAVALI',        'Gundavali',                'Andheri East',     'OPEN'),
    ('MOGRA',            'Mogra',                    'Andheri East',     'OPEN'),
    ('JOGESHWARI_EAST',  'Jogeshwari (East)',        'Jogeshwari East',  'OPEN'),
    ('GOREGAON_EAST',    'Goregaon (East)',          'Goregaon East',    'OPEN'),
    ('AAREY',            'Aarey',                    'Goregaon East',    'OPEN'),
    ('DINDOSHI',         'Dindoshi',                 'Malad East',       'OPEN'),
    ('KURAR',            'Kurar',                    'Malad East',       'OPEN'),
    ('AKURLI',           'Akurli',                   'Kandivali East',   'OPEN'),
    ('POISAR',           'Poisar',                   'Kandivali East',   'OPEN'),
    ('MAGATHANE',        'Magathane',                'Borivali East',    'OPEN'),
    ('DEVIPADA',         'Devipada',                 'Borivali East',    'OPEN'),
    ('RASHTRIYA_UDYAN',  'Rashtriya Udyan',          'Borivali East',    'OPEN'),
    ('OVARIPADA',        'Ovaripada',                'Dahisar East',     'OPEN')
ON CONFLICT (code) DO NOTHING;

-- Line membership in operator order. Sequence numbers start at 1 at the first-listed terminal.
INSERT INTO line_station (line_id, station_id, sequence_no)
SELECT l.id, s.id, v.seq
FROM (VALUES
    ('1', 'VERSOVA', 1), ('1', 'DN_NAGAR', 2), ('1', 'AZAD_NAGAR', 3), ('1', 'ANDHERI', 4),
    ('1', 'WEH', 5), ('1', 'CHAKALA', 6), ('1', 'AIRPORT_ROAD', 7), ('1', 'MAROL_NAKA', 8),
    ('1', 'SAKI_NAKA', 9), ('1', 'ASALPHA', 10), ('1', 'JAGRUTI_NAGAR', 11), ('1', 'GHATKOPAR', 12),

    ('2A', 'DAHISAR_EAST', 1), ('2A', 'ANAND_NAGAR', 2), ('2A', 'KANDARPADA', 3), ('2A', 'MANDAPESHWAR', 4),
    ('2A', 'EKSAR', 5), ('2A', 'BORIVALI_WEST', 6), ('2A', 'SHIMPOLI', 7), ('2A', 'KANDIVALI_WEST', 8),
    ('2A', 'DAHANUKARWADI', 9), ('2A', 'VALNAI', 10), ('2A', 'MALAD_WEST', 11), ('2A', 'LOWER_MALAD', 12),
    ('2A', 'BANGUR_NAGAR', 13), ('2A', 'GOREGAON_WEST', 14), ('2A', 'OSHIWARA', 15), ('2A', 'LOWER_OSHIWARA', 16),
    ('2A', 'ANDHERI_WEST', 17),

    ('7', 'GUNDAVALI', 1), ('7', 'MOGRA', 2), ('7', 'JOGESHWARI_EAST', 3), ('7', 'GOREGAON_EAST', 4),
    ('7', 'AAREY', 5), ('7', 'DINDOSHI', 6), ('7', 'KURAR', 7), ('7', 'AKURLI', 8),
    ('7', 'POISAR', 9), ('7', 'MAGATHANE', 10), ('7', 'DEVIPADA', 11), ('7', 'RASHTRIYA_UDYAN', 12),
    ('7', 'OVARIPADA', 13), ('7', 'DAHISAR_EAST', 14)
) AS v(line_code, station_code, seq)
JOIN metro_line l ON l.line_code = v.line_code
JOIN station s ON s.code = v.station_code
ON CONFLICT (line_id, station_id) DO NOTHING;

-- Ride edges between consecutive stops, created in BOTH directions (estimate: 2 minutes per hop).
INSERT INTO network_edge (from_node_id, to_node_id, edge_type, base_minutes, note)
SELECT a.id, b.id, 'RIDE', 2, 'Prototype estimate'
FROM line_station a
JOIN line_station b ON b.line_id = a.line_id AND b.sequence_no = a.sequence_no + 1
ON CONFLICT (from_node_id, to_node_id, edge_type) DO NOTHING;

INSERT INTO network_edge (from_node_id, to_node_id, edge_type, base_minutes, note)
SELECT b.id, a.id, 'RIDE', 2, 'Prototype estimate'
FROM line_station a
JOIN line_station b ON b.line_id = a.line_id AND b.sequence_no = a.sequence_no + 1
ON CONFLICT (from_node_id, to_node_id, edge_type) DO NOTHING;

-- Interchange (transfer) edges, both directions. Walking times are prototype estimates.
INSERT INTO network_edge (from_node_id, to_node_id, edge_type, base_minutes, note)
SELECT fa.id, tb.id, 'TRANSFER', v.minutes, v.note
FROM (VALUES
    ('1',  'DN_NAGAR',     '2A', 'ANDHERI_WEST', 8, 'Walkway between D N Nagar (Line 1) and Andheri (West) (Line 2A)'),
    ('2A', 'ANDHERI_WEST', '1',  'DN_NAGAR',     8, 'Walkway between Andheri (West) (Line 2A) and D N Nagar (Line 1)'),
    ('1',  'WEH',          '7',  'GUNDAVALI',    8, 'Walkway between Western Express Highway (Line 1) and Gundavali (Line 7)'),
    ('7',  'GUNDAVALI',    '1',  'WEH',          8, 'Walkway between Gundavali (Line 7) and Western Express Highway (Line 1)'),
    ('2A', 'DAHISAR_EAST', '7',  'DAHISAR_EAST', 4, 'Change between Line 2A and Line 7 within Dahisar (East)'),
    ('7',  'DAHISAR_EAST', '2A', 'DAHISAR_EAST', 4, 'Change between Line 7 and Line 2A within Dahisar (East)')
) AS v(from_line, from_station, to_line, to_station, minutes, note)
JOIN metro_line fl ON fl.line_code = v.from_line
JOIN station fs ON fs.code = v.from_station
JOIN line_station fa ON fa.line_id = fl.id AND fa.station_id = fs.id
JOIN metro_line tl ON tl.line_code = v.to_line
JOIN station ts ON ts.code = v.to_station
JOIN line_station tb ON tb.line_id = tl.id AND tb.station_id = ts.id
ON CONFLICT (from_node_id, to_node_id, edge_type) DO NOTHING;
