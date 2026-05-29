**Finance Portal**

**Proje Nedir?**
Finance Portal, kullanıcıların finansal enstrümanlarını (hisse senedi, döviz, kripto, tahvil, kıymetli maden, fon, VİOP vadeli işlemler) takip edebildiği, portföylerini yönetebildiği, finans ile ilgili haberler takip edebildiği, finansal enstrümanları karşılaştırabildiği ve fiyat alarmı kurebildiği full-stack bir web uygulamasıdır.

**Teknoloji Stack**
Katman                    Teknoloji
Frontend               React.js + Tailwind CSS
Backend                Java 21 + Spring Boot 3.2.5
Veritabanı             PostgreSQL 16
Cache                  Redis 7
Auth                   Keycloak 23.0 + OpenLDAP
LDAP UI                phpLDAPadmin
Mesajlaşma             Apache Kafka
Log Pipeline           Log4j2 → Kafka → Logstash → OpenSearch
Log UI                 OpenSearch Dashboards
İzleme                 Prometheus + Grafana
Tracing                OpenTelemetry + Data Prepper
Email                  Mailhog
Migration              Flyway
Dokümantasyon          Swagger/OpenAPI

**Gereksinimler**

1.Docker Desktop (en az 8GB RAM ayrılması önerilir)
2.Git

**Kurulum ve Çalıştırma**

1. Repoyu klonla:
    git clone https://github.com/YukselMertKirdar67/finance-portal
    cd finance-portal
2. Docker container'larını başlat:
    docker-compose up --build

  İlk başlatmada Keycloak'ın ayağa kalkması 2-3 dakika sürebilir.
  
3. Keycloak realm import:
      Uygulama ilk kez başlatıldığında Keycloak'a finance-portal realm'ini manuel import etmen gerekebilir:

      http://localhost:8180 adresine git
      Admin paneline gir (admin/admin)
      Sol üstten finance-portal realm'ini oluştur
      Realms Setting → Action → Partial Import → keycloak-realm-export.json dosyasını yükle


**Servis URL'leri**
Servis                        URL                                Kullanıcı Adı                   Şifre
Frontend               http://localhost:3000                          —                            —
Backend API            http://localhost:8080/api/v1                   —                            —
Swagger UI             http://localhost:8080/swagger-ui.html          —                            —
Keycloak               http://localhost:8180                        admin                       admin
OpenSearch Dashboards  http://localhost:5601                          —                            —
Prometheus             http://localhost:9090                          —                            —
Grafana                http://localhost:3001                        admin                       admin
Mailhog                http://localhost:8025                          —                            —
phpLDAPadmin           http://localhost:8090                 cn=admin,dc=financeportal,dc=com  admin123

**Varsayılan Kullanıcılar**
Keycloak'ta iki varsayılan kullanıcı tanımlıdır:
 Kullanıcı             Şifre       Rol
 adminuser           123456      ADMIN
 Yuksel123           123456       USER

NOT: Bu kullanıcılar 2FA (TOTP) ile tanımlanmıştır. Giriş yaparken Google Authenticator veya benzeri bir uygulama ile doğrulama kodu gerekir. Bu yüzden yeni bir kullanıcı oluşturarak giriş yapmanız önerilir.

**Yeni Kullanıcı Oluşturma ve Giriş**

  1. http://localhost:3000 adresine git
  2. Kayıt Ol butonuna tıkla
  3. Kullanıcı adı, email ve şifre gir
  4. Kayıt sonrası Mailhog'dan email doğrulaması yap:
        http://localhost:8025 adresine git
        Gelen doğrulama emailini aç ve linke tıkla
  5. Email doğrulandıktan sonra otomatik olarak 2FA kurulum sayfasına yönlendirilirsin
  6. Google Authenticator, Authy veya benzeri bir uygulama ile QR kodu tarat
  7. 2FA kurulumu tamamlandıktan sonra giriş yapabilirsin
  8. Artık her girişte kullanıcı adı + şifre + 2FA kodu istenir

**2FA (İki Faktörlü Kimlik Doğrulama)**
2FA zorunludur, her kullanıcı için aktiftir. Giriş sırasında otomatik olarak TOTP kodu istenir. Google Authenticator veya Authy gibi bir uygulama gereklidir.


**Özellikler**

  * Finansal Enstrümanlar — Döviz, hisse, kripto, tahvil, kıymetli maden, fon, VİOP vadeli işlemler
  * Veri Kaynakları — TCMB, Yahoo Finance, İş Yatırım, TCMB EVDS
  * Portföy Yönetimi — Portföy oluşturma, alış/satış işlemleri, kar/zarar takibi
  * Fiyat Alarmları — Hedef fiyat belirle, uygulama bildirimi al
  * Takip Listesi — İlgilendiğin enstrümanları takibe al
  * Tarihsel Fiyat Grafikleri — Mum ve alan grafiği ile geçmiş fiyat analizi
  * Anlık Fiyat Güncellemeleri — WebSocket ile sayfayı yenilemeden fiyat takibi
  * Haberler — Finansal haberler
  * Bildirim Sistemi — Uygulama içi bildirimler
  * Kullanıcı Profili — Profil görüntüleme, tema ayarı, bildirim tercihleri
  * Email Doğrulama — Kayıt sonrası Mailhog üzerinden email doğrulama
  * 2FA — TOTP ile zorunlu iki faktörlü kimlik doğrulama
  * Admin Paneli — Fiyat güncelleme, kullanıcı yönetimi
  * Monitoring — Prometheus + Grafana ile JVM ve uygulama metrikleri
  * Log Yönetimi — OpenSearch Dashboards ile merkezi log takibi
  * Cache — Redis ile yüksek performanslı veri erişimi

**API Dokümantasyonu**
 Swagger UI: http://localhost:8080/swagger-ui.html
 Tüm endpoint'ler /api/v1/ prefix'i ile başlar.

**Monitoring**
 Grafana ve Prometheus otomatik olarak yapılandırılmıştır. docker-compose up --build sonrası hazır gelir.
 
 Grafana Dashboard:

  1. http://localhost:3001 adresine git (admin/admin)
  2. Dashboards menüsünden JVM (Micrometer) dashboard'unu aç
  3. Application dropdown → finance-backend seç
  4. Instance dropdown → finance-backend:8080 seç

 Prometheus:

  1. http://localhost:9090 adresine git
  2. Targets sayfasında finance-backend UP görünmeli

 OpenSearch Logs:

  1. http://localhost:5601 adresine git
  2. Discover sekmesinden uygulama loglarını görüntüle


**Proje Yapısı**

finance-portal/
├── docker-compose.yml
├── prometheus.yml
├── logstash.conf
├── otel-collector-config.yml
├── data-prepper-config.yml
├── data-prepper-server.yml
├── init-keycloak-db.sql
├── keycloak-realm-export.json
├── README.md
├── grafana/
│   └── provisioning/
│       ├── dashboards/
│       │   ├── dashboard.yml
│       │   └── jvm-micrometer.json
│       └── datasources/
│           └── prometheus.yml
├── log/
│   └── log-consumer/
├── otel/
├── backend/
│   └── finance-portal-backend/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/financeportal/backend/
│       │   │   │   ├── Aspect/
│       │   │   │   ├── Comparison/
│       │   │   │   ├── Config/
│       │   │   │   ├── Email/
│       │   │   │   ├── Exception/
│       │   │   │   ├── Home/
│       │   │   │   ├── Instrument/
│       │   │   │   ├── News/
│       │   │   │   ├── Notification/
│       │   │   │   ├── Portfolio/
│       │   │   │   ├── PriceAlert/
│       │   │   │   ├── Totp/
│       │   │   │   ├── User/
│       │   │   │   ├── Util/
│       │   │   │   ├── Watchlist/
│       │   │   │   └── WebSocket/
│       │   │   └── resources/
│       │   │       ├── db/migration/
│       │   │       │   ├── V1__baseline.sql
│       │   │       │   └── V2__add_viop.sql
│       │   │       ├── application.properties
│       │   │       └── log4j2-spring.xml
│       │   └── test/
│       ├── pom.xml
│       └── Dockerfile
└── frontend/
└── finance-portal-frontend/
├── src/
│   ├── API/
│   ├── assets/
│   ├── Components/
│   │   ├── Layout/
│   │   ├── Page/
│   │   └── UI/
│   ├── context/
│   └── Hooks/
├── public/
├── nginx.conf
├── package.json
├── vite.config.js
└── Dockerfile
