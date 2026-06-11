const express = require('express');
const { Pool } = require('pg');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');
const session = require('express-session');

const app = express();
app.use(cors());
app.use(express.json());
app.use(session({ secret: 'admin123', resave: false, saveUninitialized: true }));

// ==================== КОНФИГУРАЦИЯ ====================
const JWT_SECRET = 'evacuation_secret_key_2025';

// PostgreSQL
const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'evacuation_db',
    password: 'postgres',
    port: 5432,
});

// Nodemailer (Яндекс)
const transporter = nodemailer.createTransport({
    host: 'smtp.yandex.ru',
    port: 587,
    secure: false,
    auth: {
        user: 'KekMem11@yandex.ru',
        pass: 'wwouccdqktdjsndu'
    },
    tls: { rejectUnauthorized: false }
});

// Хранилище кодов подтверждения
const codeStore = new Map();

// ==================== ИНИЦИАЛИЗАЦИЯ БД ====================
async function initDB() {
    try {
        await pool.query(`
            CREATE TABLE IF NOT EXISTS users (
                user_id SERIAL PRIMARY KEY,
                phone VARCHAR(20) UNIQUE NOT NULL,
                role VARCHAR(10) NOT NULL CHECK (role IN ('client', 'driver')),
                name VARCHAR(100),
                email VARCHAR(100),
                registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP
            )
        `);
        await pool.query(`
            CREATE TABLE IF NOT EXISTS drivers (
                driver_id INTEGER PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
                is_online BOOLEAN DEFAULT FALSE,
                car_model VARCHAR(100),
                car_number VARCHAR(20),
                rating DECIMAL(3,2) DEFAULT 0.0,
                total_orders INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        await pool.query(`
            CREATE TABLE IF NOT EXISTS orders (
                order_id SERIAL PRIMARY KEY,
                client_id INTEGER NOT NULL REFERENCES users(user_id),
                driver_id INTEGER REFERENCES users(user_id),
                status VARCHAR(20) DEFAULT 'waiting' CHECK (status IN ('waiting','accepted','in_progress','completed','cancelled')),
                pickup_address TEXT NOT NULL,
                dropoff_address TEXT NOT NULL,
                price DECIMAL(10,2) DEFAULT 1000.00,
                contact_phone VARCHAR(20),
                comment TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                completed_at TIMESTAMP
            )
        `);
        await pool.query(`
            CREATE TABLE IF NOT EXISTS driver_locations (
                id SERIAL PRIMARY KEY,
                driver_id INTEGER UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                latitude DECIMAL(10,8) NOT NULL,
                longitude DECIMAL(11,8) NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        console.log('✅ Таблицы созданы / уже существуют');
    } catch (err) {
        console.error('❌ Ошибка инициализации БД:', err);
    }
}
initDB();

// ==================== ФОРМАТТЕРЫ ====================
function formatOrder(row) {
    if (!row) return null;
    return {
        orderId: row.order_id,
        clientId: row.client_id,
        driverId: row.driver_id,
        status: row.status,
        pickupAddress: row.pickup_address,
        dropoffAddress: row.dropoff_address,
        price: parseFloat(row.price),
        createdAt: row.created_at,
        completedAt: row.completed_at,
        contactPhone: row.contact_phone,
        comment: row.comment
    };
}

function formatUser(row) {
    if (!row) return null;
    return {
        userId: row.user_id,
        phone: row.phone,
        role: row.role,
        name: row.name,
        email: row.email,
        registeredAt: row.registered_at,
        lastLogin: row.last_login
    };
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ МАРШРУТЫ ====================
// Проверка существования пользователя
app.get('/api/auth/check/:phone', async (req, res) => {
    const { phone } = req.params;
    try {
        const result = await pool.query('SELECT role, email FROM users WHERE phone = $1', [phone]);
        if (result.rows.length > 0) {
            res.json({ exists: true, role: result.rows[0].role, email: result.rows[0].email });
        } else {
            res.json({ exists: false });
        }
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка проверки' });
    }
});

// Получение пользователя по ID
app.get('/api/users/:userId', async (req, res) => {
    const { userId } = req.params;
    try {
        const result = await pool.query('SELECT user_id, phone, name, email FROM users WHERE user_id = $1', [userId]);
        if (result.rows.length === 0) return res.status(404).json({ error: 'Пользователь не найден' });
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения пользователя' });
    }
});

// ==================== АУТЕНТИФИКАЦИЯ ====================
// Отправка кода подтверждения
app.post('/api/auth/send-code', async (req, res) => {
    const { phone, name, email, role } = req.body;
    if (!phone) return res.status(400).json({ error: 'Телефон обязателен' });

    const code = Math.floor(100000 + Math.random() * 900000).toString();
    let userEmail = email;
    let userName = name;

    if (!email) {
        const user = await pool.query('SELECT email, name FROM users WHERE phone = $1', [phone]);
        if (user.rows.length > 0) {
            userEmail = user.rows[0].email;
            userName = user.rows[0].name;
        }
    }

    if (!userEmail) {
        return res.status(400).json({ error: 'У пользователя не указан email' });
    }

    codeStore.set(phone, { code, name: userName, role, phone, email: userEmail, expires: Date.now() + 5 * 60 * 1000 });

    try {
        await transporter.sendMail({
            from: '"Автоэвакуатор" <KekMem11@yandex.ru>',
            to: userEmail,
            subject: 'Код подтверждения — Автоэвакуатор',
            text: `Здравствуйте, ${userName || 'пользователь'}!\n\nВаш код для входа: ${code}\nКод действителен 5 минут.`
        });
        console.log(`✅ Код для ${phone} отправлен на ${userEmail}: ${code}`);
        return res.json({ success: true });  // ← важно: return
    } catch (err) {
        console.error('❌ Ошибка отправки письма:', err.message);
        return res.status(500).json({ error: 'Не удалось отправить код на почту' });
    }
});

// Подтверждение кода
app.post('/api/auth/verify-code', async (req, res) => {
    const { phone, code } = req.body;
    const record = codeStore.get(phone);
    if (!record || record.code !== code || record.expires < Date.now()) {
        return res.status(400).json({ error: 'Неверный или просроченный код' });
    }
    codeStore.delete(phone);
    res.json({ success: true });
});

// Логин / регистрация
app.post('/api/auth/login', async (req, res) => {
    const { phone, role, name, email } = req.body;
    
    // Проверка обязательных полей
    if (!phone || !role) {
        return res.status(400).json({ error: 'Телефон и роль обязательны' });
    }
    
    try {
        // Проверяем, существует ли пользователь с таким номером телефона
        let user = await pool.query('SELECT * FROM users WHERE phone = $1', [phone]);
        
        if (user.rows.length === 0) {
            // ========== НОВЫЙ ПОЛЬЗОВАТЕЛЬ ==========
            
            // Проверка имени
            if (!name || name.trim() === '') {
                return res.status(400).json({ error: 'Имя пользователя обязательно' });
            }
            
            // Проверка email (для клиента)
            if (role === 'client' && (!email || email.trim() === '')) {
                return res.status(400).json({ error: 'Email обязателен для регистрации' });
            }
            
            // Проверка уникальности email (если email передан)
            if (email && email.trim() !== '') {
                const emailExists = await pool.query('SELECT user_id FROM users WHERE email = $1', [email.trim()]);
                if (emailExists.rows.length > 0) {
                    return res.status(400).json({ error: 'Пользователь с таким email уже существует' });
                }
            }
            
            // Создаём нового пользователя
            const newUser = await pool.query(
                `INSERT INTO users (phone, role, name, email, last_login)
                 VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP) RETURNING *`,
                [phone, role, name.trim(), email?.trim() || '']
            );
            user = newUser.rows[0];
            
            // Если роль driver – создаём запись в таблице drivers
            if (role === 'driver') {
                await pool.query('INSERT INTO drivers (driver_id) VALUES ($1)', [user.user_id]);
            }
        } else {
            // ========== СУЩЕСТВУЮЩИЙ ПОЛЬЗОВАТЕЛЬ ==========
            user = user.rows[0];
            
            // Обновляем роль, если она изменилась
            if (user.role !== role) {
                await pool.query('UPDATE users SET role = $1 WHERE user_id = $2', [role, user.user_id]);
                user.role = role;
            }
            
            // Обновляем email, если он передан и отличается
            if (email && email.trim() !== '' && user.email !== email.trim()) {
                // Проверяем, что новый email не занят другим пользователем
                const emailExists = await pool.query(
                    'SELECT user_id FROM users WHERE email = $1 AND user_id != $2', 
                    [email.trim(), user.user_id]
                );
                if (emailExists.rows.length > 0) {
                    return res.status(400).json({ error: 'Пользователь с таким email уже существует' });
                }
                await pool.query('UPDATE users SET email = $1 WHERE user_id = $2', [email.trim(), user.user_id]);
                user.email = email.trim();
            }
            
            // Обновляем время последнего входа
            await pool.query('UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = $1', [user.user_id]);
        }
        
        // Генерируем JWT-токен
        const token = jwt.sign(
            { userId: user.user_id, phone: user.phone, role: user.role },
            JWT_SECRET,
            { expiresIn: '7d' }
        );
        
        // Возвращаем ответ
        return res.json({ token, user: formatUser(user) });
        
    } catch (err) {
        console.error('Ошибка в /api/auth/login:', err);
        
        // Обработка ошибки уникальности телефона (на всякий случай)
        if (err.code === '23505') {
            return res.status(400).json({ error: 'Пользователь с таким номером телефона уже существует' });
        }
        
        return res.status(500).json({ error: 'Ошибка сервера' });
    }
});

// ==================== ЗАКАЗЫ ====================
// 1. ДОСТУПНЫЕ ЗАКАЗЫ 
app.get('/api/orders/available', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM orders WHERE status = $1 ORDER BY created_at ASC', ['waiting']);
        res.json(result.rows.map(formatOrder));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения доступных заказов' });
    }
});

// 2. Получение заказа по ID (динамический маршрут)
app.get('/api/orders/:orderId', async (req, res) => {
    const { orderId } = req.params;
    if (isNaN(orderId)) return res.status(400).json({ error: 'Неверный ID заказа' });
    try {
        const result = await pool.query('SELECT * FROM orders WHERE order_id = $1', [orderId]);
        if (!result.rows.length) return res.status(404).json({ error: 'Заказ не найден' });
        res.json(formatOrder(result.rows[0]));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения заказа' });
    }
});
// Создание заказа
app.post('/api/orders', async (req, res) => {
    const { clientId, pickupAddress, dropoffAddress, price, contactPhone, comment } = req.body;
    if (!clientId || !pickupAddress || !dropoffAddress) {
        return res.status(400).json({ error: 'Не все поля заполнены' });
    }
    try {
        const result = await pool.query(
            `INSERT INTO orders (client_id, pickup_address, dropoff_address, status, price, contact_phone, comment)
             VALUES ($1, $2, $3, 'waiting', $4, $5, $6) RETURNING *`,
            [clientId, pickupAddress, dropoffAddress, price || 3500, contactPhone || null, comment || null]
        );
        res.status(201).json(formatOrder(result.rows[0]));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка создания заказа' });
    }
});




// История клиента
app.get('/api/orders/client/:clientId', async (req, res) => {
    const { clientId } = req.params;
    try {
        const result = await pool.query('SELECT * FROM orders WHERE client_id = $1 ORDER BY created_at DESC', [clientId]);
        res.json(result.rows.map(formatOrder));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения истории клиента' });
    }
});

// История водителя
app.get('/api/orders/driver/:driverId', async (req, res) => {
    const { driverId } = req.params;
    try {
        const result = await pool.query('SELECT * FROM orders WHERE driver_id = $1 ORDER BY created_at DESC', [driverId]);
        res.json(result.rows.map(formatOrder));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения истории водителя' });
    }
});

// Принятие заказа
app.put('/api/orders/:orderId/accept', async (req, res) => {
    const { orderId } = req.params;
    const { driverId } = req.body;
    if (!driverId) return res.status(400).json({ error: 'driverId обязателен' });
    if (isNaN(orderId)) return res.status(400).json({ error: 'Неверный ID заказа' });
    try {
        const result = await pool.query(
            `UPDATE orders SET driver_id = $1, status = 'accepted'
             WHERE order_id = $2 AND status = 'waiting' RETURNING *`,
            [driverId, orderId]
        );
        if (!result.rows.length) return res.status(400).json({ error: 'Заказ уже принят или не существует' });
        res.json(formatOrder(result.rows[0]));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка принятия заказа' });
    }
});

// Обновление статуса заказа
app.put('/api/orders/:orderId/status', async (req, res) => {
    const { orderId } = req.params;
    const { status } = req.body;
    const allowed = ['in_progress', 'completed', 'cancelled'];
    if (!allowed.includes(status)) return res.status(400).json({ error: 'Недопустимый статус' });
    if (isNaN(orderId)) return res.status(400).json({ error: 'Неверный ID заказа' });
    try {
        let query = 'UPDATE orders SET status = $1 WHERE order_id = $2 RETURNING *';
        let params = [status, orderId];
        if (status === 'completed') {
            query = 'UPDATE orders SET status = $1, completed_at = CURRENT_TIMESTAMP WHERE order_id = $2 RETURNING *';
        }
        const result = await pool.query(query, params);
        if (!result.rows.length) return res.status(404).json({ error: 'Заказ не найден' });
        res.json(formatOrder(result.rows[0]));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка обновления статуса' });
    }
});

// Отмена заказа клиентом
app.put('/api/orders/:orderId/cancel', async (req, res) => {
    const { orderId } = req.params;
    try {
        const result = await pool.query(
            `UPDATE orders SET status = 'cancelled'
             WHERE order_id = $1 AND status = 'waiting' RETURNING *`,
            [orderId]
        );
        if (result.rows.length === 0) {
            return res.status(400).json({ error: 'Заказ нельзя отменить (уже принят или завершён)' });
        }
        res.json(formatOrder(result.rows[0]));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка отмены заказа' });
    }
});

// Отмена заказа водителем
app.put('/api/orders/:orderId/cancel-by-driver', async (req, res) => {
    const { orderId } = req.params;
    const { driverId } = req.body;
    if (!driverId) return res.status(400).json({ error: 'driverId обязателен' });
    try {
        const result = await pool.query(
            `UPDATE orders SET driver_id = NULL, status = 'waiting'
             WHERE order_id = $1 AND driver_id = $2 AND status IN ('accepted', 'in_progress')
             RETURNING *`,
            [orderId, driverId]
        );
        if (result.rows.length === 0) {
            return res.status(400).json({ error: 'Заказ нельзя отменить (уже завершён или не ваш)' });
        }
        res.json(formatOrder(result.rows[0]));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка отмены заказа' });
    }
});

// ==================== ГЕОЛОКАЦИЯ ====================
app.post('/api/location', async (req, res) => {
    const { driverId, latitude, longitude } = req.body;
    if (!driverId || latitude === undefined || longitude === undefined) {
        return res.status(400).json({ error: 'Недостаточно данных' });
    }
    try {
        await pool.query(
            `INSERT INTO driver_locations (driver_id, latitude, longitude, updated_at)
             VALUES ($1, $2, $3, CURRENT_TIMESTAMP)
             ON CONFLICT (driver_id) DO UPDATE SET latitude = $2, longitude = $3, updated_at = CURRENT_TIMESTAMP`,
            [driverId, latitude, longitude]
        );
        res.json({ success: true });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка сохранения геолокации' });
    }
});

app.get('/api/location/:driverId', async (req, res) => {
    const { driverId } = req.params;
    try {
        const result = await pool.query('SELECT driver_id, latitude, longitude, updated_at FROM driver_locations WHERE driver_id = $1', [driverId]);
        if (!result.rows.length) return res.status(404).json({ error: 'Геолокация не найдена' });
        const loc = result.rows[0];
        res.json({
            driverId: loc.driver_id,
            latitude: parseFloat(loc.latitude),
            longitude: parseFloat(loc.longitude),
            updatedAt: loc.updated_at
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения геолокации' });
    }
});

// Получение всех активных водителей с геолокацией (для карты админа)
app.get('/api/admin/active-drivers', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT u.user_id, u.name, u.phone, d.is_online, dl.latitude, dl.longitude, dl.updated_at
            FROM users u
            JOIN drivers d ON u.user_id = d.driver_id
            LEFT JOIN driver_locations dl ON u.user_id = dl.driver_id
            WHERE d.is_online = true
              AND dl.updated_at > NOW() - INTERVAL '2 minutes'
        `);
        res.json(result.rows);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения данных водителей' });
    }
});
// Автоматический сброс статуса водителей, у которых давно не было геолокации
setInterval(async () => {
    try {
        await pool.query(`
            UPDATE drivers 
            SET is_online = false 
            WHERE driver_id IN (
                SELECT d.driver_id 
                FROM drivers d
                LEFT JOIN driver_locations dl ON d.driver_id = dl.driver_id
                WHERE d.is_online = true 
                  AND (dl.updated_at IS NULL OR dl.updated_at < NOW() - INTERVAL '2 minutes')
            )
        `);
    } catch (err) {
        console.error('Ошибка сброса статуса:', err);
    }
}, 60000); // каждую минуту
// ==================== ИНФОРМАЦИЯ О ВОДИТЕЛЕ ====================
app.get('/api/drivers/:driverId', async (req, res) => {
    const { driverId } = req.params;
    try {
        const result = await pool.query(
            'SELECT u.name, d.car_model, d.car_number, d.rating FROM users u JOIN drivers d ON u.user_id = d.driver_id WHERE u.user_id = $1',
            [driverId]
        );
        if (!result.rows.length) return res.status(404).json({ error: 'Водитель не найден' });
        res.json(result.rows[0]);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения данных водителя' });
    }
});

// ==================== АДМИНКА ====================
app.get('/admin/login', (req, res) => {
    res.send(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Вход в админку</title>
            <style>
                body { font-family: Arial; margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; height: 100vh; background: #f0f2f5; }
                .login-container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); width: 300px; text-align: center; }
                input { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 5px; }
                button { width: 100%; padding: 10px; background: #2196F3; color: white; border: none; border-radius: 5px; cursor: pointer; }
                button:hover { background: #1976D2; }
            </style>
        </head>
        <body>
            <div class="login-container">
                <h2>Вход в админку</h2>
                <form method="post" action="/admin/login">
                    <input name="username" placeholder="Логин" value="admin">
                    <input type="password" name="password" placeholder="Пароль" value="admin123">
                    <button type="submit">Войти</button>
                </form>
            </div>
        </body>
        </html>
    `);
});

app.post('/admin/login', express.urlencoded({ extended: true }), (req, res) => {
    if (req.body.username === 'admin' && req.body.password === 'admin123') {
        req.session.admin = true;
        res.redirect('/admin');
    } else {
        res.send('<h3>Неверные данные</h3><a href="/admin/login">Назад</a>');
    }
});

function checkAdmin(req, res, next) {
    if (req.session.admin) return next();
    res.redirect('/admin/login');
}

// Главная страница админки с картой и статистикой
app.get('/admin', checkAdmin, async (req, res) => {
    try {
        const users = await pool.query('SELECT user_id, phone, role, name, email FROM users');
        const orders = await pool.query('SELECT * FROM orders ORDER BY created_at DESC');
        const drivers = await pool.query('SELECT d.driver_id, d.is_online, u.name FROM drivers d JOIN users u ON d.driver_id = u.user_id');
        
        // Статистика за сегодня
        const todayStats = await pool.query(`
            SELECT 
                COUNT(*) as total_orders,
                SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) as completed,
                SUM(price) as revenue
            FROM orders 
            WHERE DATE(created_at) = CURRENT_DATE
        `);
        
        // Статистика за всё время
        const allTimeStats = await pool.query(`
            SELECT 
                COUNT(*) as total_orders,
                SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) as completed,
                SUM(price) as revenue
            FROM orders
        `);
        
        // Статистика по каждому водителю за сегодня
        const driverStats = await pool.query(`
            SELECT u.name, u.user_id, COUNT(o.order_id) as orders_count, SUM(o.price) as total_earned
            FROM users u
            LEFT JOIN orders o ON u.user_id = o.driver_id AND DATE(o.created_at) = CURRENT_DATE AND o.status = 'completed'
            WHERE u.role = 'driver'
            GROUP BY u.user_id, u.name
            ORDER BY orders_count DESC
        `);

        res.send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>Админ-панель — Автоэвакуатор</title>
                <meta charset="utf-8">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    * { box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
                    .header { background: linear-gradient(135deg, #2196F3, #1976D2); color: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; }
                    h1 { margin: 0; font-size: 24px; }
                    .logout { float: right; color: white; text-decoration: none; background: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 5px; }
                    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px; }
                    .stat-card { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); text-align: center; }
                    .stat-card h3 { margin: 0 0 10px; color: #666; font-size: 14px; }
                    .stat-card .value { font-size: 28px; font-weight: bold; color: #2196F3; }
                    .section { background: white; border-radius: 10px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
                    .section h2 { margin-top: 0; border-bottom: 2px solid #2196F3; padding-bottom: 10px; }
                    #map { height: 500px; border-radius: 10px; margin-top: 15px; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background: #f0f0f0; }
                    .status-waiting { color: orange; }
                    .status-accepted { color: blue; }
                    .status-in_progress { color: purple; }
                    .status-completed { color: green; }
                    .online { color: green; font-weight: bold; }
                    .offline { color: gray; }
                    .export-btn { background: #4CAF50; color: white; border: none; padding: 8px 16px; border-radius: 5px; cursor: pointer; margin-bottom: 15px; }
                    .export-btn:hover { background: #45a049; }
                </style>
            </head>
            <body>
                <div class="header">
                    <a href="/admin/logout" class="logout">Выйти</a>
                    <h1>🚚 Админ-панель — Автоэвакуатор</h1>
                </div>

                <div class="stats-grid">
                    <div class="stat-card">
                        <h3>📊 Заказов сегодня</h3>
                        <div class="value">${todayStats.rows[0].total_orders}</div>
                    </div>
                    <div class="stat-card">
                        <h3>✅ Выполнено сегодня</h3>
                        <div class="value">${todayStats.rows[0].completed}</div>
                    </div>
                    <div class="stat-card">
                        <h3>💰 Выручка сегодня</h3>
                        <div class="value">${todayStats.rows[0].revenue || 0} ₽</div>
                    </div>
                    <div class="stat-card">
                        <h3>📈 Всего заказов</h3>
                        <div class="value">${allTimeStats.rows[0].total_orders}</div>
                    </div>
                    <div class="stat-card">
                        <h3>🏆 Всего выручка</h3>
                        <div class="value">${allTimeStats.rows[0].revenue || 0} ₽</div>
                    </div>
                </div>

                <div class="section">
                    <h2>🗺️ Активные эвакуаторы на карте</h2>
                    <div id="map"></div>
                    <script>
    var map = L.map('map').setView([58.0105, 56.2502], 12);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>'
    }).addTo(map);

    var markers = {};

    function updateDriversOnMap() {
        fetch('/api/admin/active-drivers')
            .then(function(res) { return res.json(); })
            .then(function(drivers) {
                // Удаляем старые маркеры
                for (var id in markers) {
                    var found = false;
                    for (var i = 0; i < drivers.length; i++) {
                        if (drivers[i].user_id == id) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        map.removeLayer(markers[id]);
                        delete markers[id];
                    }
                }

                // Добавляем или обновляем маркеры
                for (var i = 0; i < drivers.length; i++) {
                    var driver = drivers[i];
                    if (driver.latitude && driver.longitude) {
                        if (markers[driver.user_id]) {
                            markers[driver.user_id].setLatLng([driver.latitude, driver.longitude]);
                        } else {
                            var marker = L.marker([driver.latitude, driver.longitude]).addTo(map);
                            var popupText = '<b>' + (driver.name || 'Водитель') + '</b><br>' +
                                '📞 ' + (driver.phone || '—') + '<br>' +
                                '🟢 На линии<br>' +
                                '🕐 Обновлено: ' + new Date(driver.updated_at).toLocaleTimeString();
                            marker.bindPopup(popupText);
                            markers[driver.user_id] = marker;
                        }
                    }
                }

                if (drivers.length === 0) {
                    document.getElementById('map').innerHTML = '<p style="text-align:center; padding:50px;">Нет активных водителей на линии</p>';
                }
            })
            .catch(function(err) {
                console.error('Ошибка загрузки:', err);
            });
    }

    updateDriversOnMap();
    setInterval(updateDriversOnMap, 10000);
</script>
                </div>

                <div class="section">
                    <h2>📋 Статистика по водителям (сегодня)</h2>
                    <button class="export-btn" onclick="exportDriverStats()">📎 Выгрузить статистику водителей (CSV)</button>
                    <table>
                        <thead>
                            <tr><th>Водитель</th><th>Кол-во заказов</th><th>Заработано</th></tr>
                        </thead>
                        <tbody>
                            ${driverStats.rows.map(d => `
                                <tr><td>${d.name || 'Водитель #' + d.user_id}</td><td>${d.orders_count || 0}</td><td>${d.total_earned || 0} ₽</td></tr>
                            `).join('')}
                            ${driverStats.rows.length === 0 ? '<tr><td colspan="3">Нет данных</td></tr>' : ''}
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>👥 Пользователи</h2>
                    <button class="export-btn" onclick="exportUsers()">📎 Выгрузить пользователей (CSV)</button>
                    <table>
                        <thead><tr><th>ID</th><th>Телефон</th><th>Роль</th><th>Имя</th><th>Email</th></tr></thead>
                        <tbody>
                            ${users.rows.map(u => `<tr><td>${u.user_id}</td><td>${u.phone}</td><td>${u.role}</td><td>${u.name || ''}</td><td>${u.email || ''}</td></tr>`).join('')}
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>🚛 Водители</h2>
                    <table>
                        <thead><tr><th>ID</th><th>Имя</th><th>Статус</th></tr></thead>
                        <tbody>
                            ${drivers.rows.map(d => `<tr><td>${d.driver_id}</td><td>${d.name || ''}</td><td class="${d.is_online ? 'online' : 'offline'}">${d.is_online ? 'На линии' : 'Не на линии'}</td></tr>`).join('')}
                        </tbody>
                    </table>
                </div>

                <div class="section">
                    <h2>📦 Заказы</h2>
                    <button class="export-btn" onclick="exportOrders()">📎 Выгрузить заказы (CSV)</button>
                    <table>
                        <thead><tr><th>ID</th><th>Клиент</th><th>Водитель</th><th>Статус</th><th>Маршрут</th><th>Цена</th></tr></thead>
                        <tbody>
                            ${orders.rows.map(o => `
                                <tr>
                                    <td>${o.order_id}</td>
                                    <td>${o.client_id}</td>
                                    <td>${o.driver_id || '—'}</td>
                                    <td class="status-${o.status}">${o.status}</td>
                                    <td>${o.pickup_address} → ${o.dropoff_address}</td>
                                    <td>${o.price} ₽</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>

                <script>
                    function exportDriverStats() {
                        window.location.href = '/admin/export/driver-stats';
                    }
                    function exportUsers() {
                        window.location.href = '/admin/export/users';
                    }
                    function exportOrders() {
                        window.location.href = '/admin/export/orders';
                    }
                </script>
            </body>
            </html>
        `);
    } catch (err) {
        console.error(err);
        res.status(500).send('Ошибка сервера');
    }
});

// Экспорт статистики водителей (CSV)
app.get('/admin/export/driver-stats', checkAdmin, async (req, res) => {
    try {
        const driverStats = await pool.query(`
            SELECT u.name, u.phone, COUNT(o.order_id) as orders_count, COALESCE(SUM(o.price), 0) as total_earned
            FROM users u
            LEFT JOIN orders o ON u.user_id = o.driver_id AND DATE(o.created_at) = CURRENT_DATE AND o.status = 'completed'
            WHERE u.role = 'driver'
            GROUP BY u.user_id, u.name, u.phone
            ORDER BY orders_count DESC
        `);
        
        let csv = "Водитель,Телефон,Кол-во заказов,Заработано\n";
        driverStats.rows.forEach(d => {
            csv += `"${d.name || 'Водитель'}","${d.phone || ''}",${d.orders_count || 0},${d.total_earned || 0}\n`;
        });
        
        res.setHeader('Content-Type', 'text/csv');
        res.setHeader('Content-Disposition', 'attachment; filename=driver_stats.csv');
        res.send(csv);
    } catch (err) {
        console.error(err);
        res.status(500).send('Ошибка экспорта');
    }
});

// Экспорт пользователей (CSV)
app.get('/admin/export/users', checkAdmin, async (req, res) => {
    try {
        const users = await pool.query('SELECT user_id, phone, role, name, email, registered_at FROM users');
        let csv = "ID,Телефон,Роль,Имя,Email,Дата регистрации\n";
        users.rows.forEach(u => {
            csv += `${u.user_id},"${u.phone}","${u.role}","${u.name || ''}","${u.email || ''}","${u.registered_at}"\n`;
        });
        res.setHeader('Content-Type', 'text/csv');
        res.setHeader('Content-Disposition', 'attachment; filename=users.csv');
        res.send(csv);
    } catch (err) {
        console.error(err);
        res.status(500).send('Ошибка экспорта');
    }
});

// Экспорт заказов (CSV)
app.get('/admin/export/orders', checkAdmin, async (req, res) => {
    try {
        const orders = await pool.query('SELECT order_id, client_id, driver_id, status, pickup_address, dropoff_address, price, created_at, completed_at FROM orders ORDER BY created_at DESC');
        let csv = "ID,Клиент,Водитель,Статус,Откуда,Куда,Цена,Создан,Завершён\n";
        orders.rows.forEach(o => {
            csv += `${o.order_id},${o.client_id},${o.driver_id || ''},"${o.status}","${o.pickup_address}","${o.dropoff_address}",${o.price},"${o.created_at}","${o.completed_at || ''}"\n`;
        });
        res.setHeader('Content-Type', 'text/csv');
        res.setHeader('Content-Disposition', 'attachment; filename=orders.csv');
        res.send(csv);
    } catch (err) {
        console.error(err);
        res.status(500).send('Ошибка экспорта');
    }
});
// Получение статуса водителя
app.get('/api/drivers/:driverId/status', async (req, res) => {
    const { driverId } = req.params;
    try {
        const result = await pool.query('SELECT is_online FROM drivers WHERE driver_id = $1', [driverId]);
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Водитель не найден' });
        }
        res.json({ isOnline: result.rows[0].is_online });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка получения статуса' });
    }
});
app.get('/admin/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/admin/login');
});
// Обновление статуса водителя (На линии / Не на линии)
app.put('/api/drivers/status', async (req, res) => {
    const { driverId, isOnline } = req.body;
    if (!driverId) return res.status(400).json({ error: 'driverId обязателен' });
    try {
        await pool.query('UPDATE drivers SET is_online = $1 WHERE driver_id = $2', [isOnline, driverId]);
        console.log(`🔄 Водитель ${driverId} ${isOnline ? 'на линии' : 'не на линии'}`);
        res.json({ success: true });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Ошибка обновления статуса' });
    }
});
// ==================== ЗАПУСК ====================
const PORT = 8080;
app.listen(PORT, () => {
    console.log(`🚀 Сервер запущен на http://localhost:${PORT}`);
    console.log(`📊 Админ-панель: http://localhost:${PORT}/admin`);
    console.log(`🔐 Логин: admin / Пароль: admin123`);
});