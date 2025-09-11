export const API_CONFIG = {
  // URL base da API - será detectada automaticamente baseada no ambiente
  baseUrl: window.location.port === '4200' 
    ? 'http://localhost:8081'  // Desenvolvimento local (Angular dev server)
    : '/api'                   // Produção/Docker - usa o proxy configurado no nginx
};
