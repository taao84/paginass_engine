-- =============================================================================
-- Seed data for local development and testing
-- Safe to run multiple times: uses ON CONFLICT DO NOTHING
-- search_vector is auto-populated by the trg_business_search_vector trigger
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USERS
-- -----------------------------------------------------------------------------
INSERT INTO users (id, oauth_provider, oauth_id, email, full_name)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'google', 'google-001', 'carlos.mendez@example.com',  'Carlos Méndez'),
    ('a1000000-0000-0000-0000-000000000002', 'google', 'google-002', 'maria.gonzalez@example.com', 'María González'),
    ('a1000000-0000-0000-0000-000000000003', 'github', 'github-003', 'roberto.jimenez@example.com','Roberto Jiménez')
ON CONFLICT DO NOTHING;


-- -----------------------------------------------------------------------------
-- BUSINESS PROFILES
-- The trigger trg_business_search_vector fires on INSERT and populates
-- search_vector automatically from name, description, tags, and contact_info.
-- -----------------------------------------------------------------------------
INSERT INTO business_profiles (id, owner_id, name, slug, biz_type, description, tags, contact_info, image_urls, is_active)
VALUES

-- ── Oficios del hogar ─────────────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000001',
    'a1000000-0000-0000-0000-000000000001',
    'Plomería Méndez',
    'plomeria-mendez',
    'BRICK_AND_MORTAR',
    'Servicio profesional de plomería para hogares y negocios. Reparación de tuberías, instalación de sanitarios, destape de drenajes y urgencias 24 horas.',
    '["plomero", "plomería", "fontanería", "tuberías", "drenaje", "sanitarios", "urgencias", "24 horas"]',
    '{"telefono": "555-1001", "whatsapp": "555-1001", "direccion": "Calle Hidalgo 45, Centro"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000002',
    'a1000000-0000-0000-0000-000000000001',
    'Electricista Rápido 24h',
    'electricista-rapido-24h',
    'GIG',
    'Instalaciones eléctricas residenciales y comerciales. Cambio de cableado, tableros eléctricos, iluminación LED, cortocircuitos y emergencias las 24 horas del día.',
    '["electricista", "electricidad", "instalaciones eléctricas", "tablero eléctrico", "cableado", "LED", "urgencias", "24 horas"]',
    '{"telefono": "555-1002", "whatsapp": "555-1002", "ciudad": "Centro"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000003',
    'a1000000-0000-0000-0000-000000000001',
    'Pintor Profesional Ramírez',
    'pintor-profesional-ramirez',
    'INFORMAL',
    'Pintura de interiores y exteriores para casas, departamentos y locales. Acabados finos, pintura epóxica, impermeabilización y decoración de paredes.',
    '["pintor", "pintura", "pintura de casas", "interiores", "exteriores", "impermeabilización", "decoración", "acabados"]',
    '{"telefono": "555-1003", "whatsapp": "555-1003"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000004',
    'a1000000-0000-0000-0000-000000000001',
    'Cerrajería Express',
    'cerrajeria-express',
    'GIG',
    'Apertura de puertas, duplicado de llaves, instalación y reparación de cerraduras de seguridad. Atendemos emergencias a domicilio en menos de 30 minutos.',
    '["cerrajero", "cerrajería", "llaves", "cerraduras", "apertura de puertas", "duplicado de llaves", "emergencias", "seguridad"]',
    '{"telefono": "555-1004", "whatsapp": "555-1004", "ciudad": "Zona Metropolitana"}',
    '[]',
    true
),

-- ── Restaurantes y alimentos ──────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000005',
    'a1000000-0000-0000-0000-000000000002',
    'Restaurante La Casa de María',
    'restaurante-la-casa-de-maria',
    'BRICK_AND_MORTAR',
    'Cocina casera y tradicional con los mejores sabores de la región. Especialidad en caldos, guisados, mole y comida del día a precio justo. Ambiente familiar.',
    '["restaurante", "comida casera", "comida tradicional", "mole", "caldos", "guisados", "almuerzo", "comida del día", "familiar"]',
    '{"telefono": "555-2001", "direccion": "Av. Juárez 120, Col. Centro", "horario": "Lun-Sáb 8am-6pm"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000006',
    'a1000000-0000-0000-0000-000000000002',
    'Pizzería Don Pepino',
    'pizzeria-don-pepino',
    'BRICK_AND_MORTAR',
    'Las mejores pizzas artesanales de la ciudad. Masa delgada o gruesa, ingredientes frescos y horno de leña. Pedidos a domicilio y para llevar.',
    '["pizza", "pizzería", "comida italiana", "pasta", "horno de leña", "domicilio", "para llevar", "artesanal"]',
    '{"telefono": "555-2002", "whatsapp": "555-2002", "direccion": "Blvd. Constitución 78", "horario": "Mar-Dom 1pm-11pm"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000007',
    'a1000000-0000-0000-0000-000000000002',
    'Panadería El Buen Pan',
    'panaderia-el-buen-pan',
    'BRICK_AND_MORTAR',
    'Panadería artesanal con más de 20 años de tradición. Pan dulce, pan de caja, pasteles personalizados para fiestas y bodas, galletas y repostería fina.',
    '["panadería", "pan dulce", "pasteles", "repostería", "galletas", "pan artesanal", "bodas", "cumpleaños", "pedidos especiales"]',
    '{"telefono": "555-2003", "direccion": "Calle Morelos 33, Barrio Antiguo", "horario": "Lun-Dom 7am-9pm"}',
    '[]',
    true
),

-- ── Salud y bienestar ─────────────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000008',
    'a1000000-0000-0000-0000-000000000003',
    'Farmacia San José',
    'farmacia-san-jose',
    'BRICK_AND_MORTAR',
    'Farmacia con amplio surtido de medicamentos de patente y genéricos. Servicio de entrega a domicilio, toma de presión, glucosa y saturación sin costo.',
    '["farmacia", "medicamentos", "genéricos", "salud", "entrega a domicilio", "presión arterial", "glucosa", "vitaminas", "suplementos"]',
    '{"telefono": "555-3001", "direccion": "Calle Independencia 9, Centro", "horario": "Lun-Dom 8am-10pm"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000009',
    'a1000000-0000-0000-0000-000000000003',
    'Clínica Dental Dr. Jiménez',
    'clinica-dental-dr-jimenez',
    'BRICK_AND_MORTAR',
    'Atención dental integral para toda la familia. Limpieza dental, extracciones, ortodoncia, blanqueamiento, implantes y prótesis. Primera consulta sin costo.',
    '["dentista", "odontología", "clínica dental", "ortodoncia", "blanqueamiento", "implantes", "limpieza dental", "prótesis", "familia"]',
    '{"telefono": "555-3002", "whatsapp": "555-3002", "direccion": "Av. Reforma 55, Piso 2", "horario": "Lun-Vie 9am-7pm, Sáb 9am-2pm"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000010',
    'a1000000-0000-0000-0000-000000000003',
    'Veterinaria Amigos Peludos',
    'veterinaria-amigos-peludos',
    'BRICK_AND_MORTAR',
    'Clínica veterinaria para perros, gatos y animales exóticos. Consultas, vacunas, cirugías, grooming y venta de alimentos y accesorios para mascotas.',
    '["veterinario", "veterinaria", "perros", "gatos", "mascotas", "vacunas", "grooming", "cirugía veterinaria", "animales", "alimento para mascotas"]',
    '{"telefono": "555-3003", "direccion": "Calle Allende 201", "horario": "Lun-Sáb 9am-7pm"}',
    '[]',
    true
),

-- ── Servicios profesionales ───────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000011',
    'a1000000-0000-0000-0000-000000000001',
    'Contabilidad y Asesoría Fiscal Torres',
    'contabilidad-asesoria-fiscal-torres',
    'GIG',
    'Servicios contables y fiscales para personas físicas y morales. Declaraciones anuales, contabilidad mensual, alta ante el SAT, facturación electrónica y asesoría en deducibles.',
    '["contador", "contabilidad", "fiscal", "declaración anual", "SAT", "impuestos", "facturación", "personas físicas", "personas morales", "RFC"]',
    '{"telefono": "555-1010", "email": "torres.contabilidad@example.com", "whatsapp": "555-1010"}',
    '[]',
    true
),
(
    'b1000000-0000-0000-0000-000000000012',
    'a1000000-0000-0000-0000-000000000002',
    'Clases de Inglés con Sofía',
    'clases-de-ingles-con-sofia',
    'GIG',
    'Clases de inglés personalizadas para niños, jóvenes y adultos. Inglés conversacional, preparación para exámenes TOEFL y IELTS, inglés de negocios. Clases presenciales y en línea.',
    '["inglés", "clases de inglés", "tutor", "idiomas", "TOEFL", "IELTS", "inglés de negocios", "conversacional", "en línea", "niños", "adultos"]',
    '{"telefono": "555-2010", "whatsapp": "555-2010", "email": "sofia.ingles@example.com"}',
    '[]',
    true
),

-- ── Automotriz ────────────────────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000013',
    'a1000000-0000-0000-0000-000000000003',
    'Taller Mecánico El Rápido',
    'taller-mecanico-el-rapido',
    'BRICK_AND_MORTAR',
    'Taller mecánico con más de 15 años de experiencia. Servicio de afinación, frenos, suspensión, transmisión, diagnóstico computarizado y cambio de aceite. Todas las marcas.',
    '["mecánico", "taller mecánico", "afinación", "frenos", "suspensión", "cambio de aceite", "diagnóstico", "transmisión", "auto", "coche", "todas las marcas"]',
    '{"telefono": "555-3010", "direccion": "Carretera Nacional Km 3, Col. Industrial", "horario": "Lun-Sáb 8am-6pm"}',
    '[]',
    true
),

-- ── Belleza y cuidado personal ────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000014',
    'a1000000-0000-0000-0000-000000000002',
    'Peluquería Estilo Moderno',
    'peluqueria-estilo-moderno',
    'BRICK_AND_MORTAR',
    'Peluquería y estética unisex. Corte de cabello, peinados, tintes, mechas, keratina, manicure y pedicure. Atendemos con cita previa y sin cita.',
    '["peluquería", "estética", "corte de cabello", "peinado", "tintes", "mechas", "keratina", "manicure", "pedicure", "unisex", "belleza"]',
    '{"telefono": "555-2011", "whatsapp": "555-2011", "direccion": "Plaza Comercial Local 14", "horario": "Mar-Dom 9am-8pm"}',
    '[]',
    true
),

-- ── Moda y ropa ───────────────────────────────────────────────────────────────

(
    'b1000000-0000-0000-0000-000000000015',
    'a1000000-0000-0000-0000-000000000002',
    'Boutique Moda Latina',
    'boutique-moda-latina',
    'BRICK_AND_MORTAR',
    'Tienda de ropa para mujer con las últimas tendencias en moda. Vestidos, blusas, pantalones, accesorios y bolsas. Tallas estándar y plus size. Envíos a todo el país.',
    '["ropa", "moda", "boutique", "vestidos", "ropa de mujer", "accesorios", "bolsas", "tallas plus", "tendencias", "envíos"]',
    '{"telefono": "555-2012", "whatsapp": "555-2012", "direccion": "Calle 5 de Mayo 88, Centro", "horario": "Lun-Sáb 10am-8pm"}',
    '[]',
    true
)

ON CONFLICT DO NOTHING;
