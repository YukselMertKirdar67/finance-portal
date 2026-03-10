import Keycloak from 'keycloak-js';
import keycloakConfig from './keycloakconfig';

// Keycloak instance oluştur
const keycloak = new Keycloak(keycloakConfig);

/**
 * Initialize Keycloak
 */
export const initKeycloak = (onAuthenticatedCallback) => {
    keycloak
        .init({
            onLoad: 'check-sso', // SSO kontrolü yap
            silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
            pkceMethod: 'S256', // PKCE güvenlik
            checkLoginIframe: false // iframe kontrolünü kapat (performance)
        })
        .then((authenticated) => {
            if (authenticated) {
                console.log('✅ User authenticated');
                // Token'ı localStorage'a kaydet
                localStorage.setItem('token', keycloak.token);
                localStorage.setItem('refreshToken', keycloak.refreshToken);
            } else {
                console.log('⚠️ User not authenticated');
            }
            onAuthenticatedCallback(authenticated);
        })
        .catch((error) => {
            console.error('❌ Keycloak init failed:', error);
        });
};

/**
 * Login - Keycloak login sayfasına yönlendir
 */
export const doLogin = () => {
    keycloak.login();
};

/**
 * Logout - Keycloak'tan çıkış yap
 */
export const doLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    keycloak.logout();
};

/**
 * Get Token
 */
export const getToken = () => keycloak.token;

/**
 * Get User Info
 */
export const getUsername = () => keycloak.tokenParsed?.preferred_username;
export const getEmail = () => keycloak.tokenParsed?.email;
export const getUserId = () => keycloak.tokenParsed?.sub;

/**
 * Check if user has role
 */
export const hasRole = (role) => {
    return keycloak.tokenParsed?.realm_access?.roles?.includes(role) || false;
};

/**
 * Is Admin
 */
export const isAdmin = () => hasRole('ADMIN');

/**
 * Update Token - Token'ı otomatik yenile
 */
export const updateToken = (successCallback) => {
    keycloak
        .updateToken(70) // 70 saniye kala yenile
        .then((refreshed) => {
            if (refreshed) {
                console.log('🔄 Token refreshed');
                localStorage.setItem('token', keycloak.token);
                localStorage.setItem('refreshToken', keycloak.refreshToken);
            }
            successCallback(keycloak.token);
        })
        .catch(() => {
            console.error('❌ Token refresh failed');
            doLogout();
        });
};

export default keycloak;