import axios from 'axios';
const api=axios.create({baseURL:'http://localhost:8080'});
api.interceptors.request.use(c=>{const token=localStorage.getItem('token');if(token)c.headers.Authorization=`Bearer ${token}`;return c});
api.interceptors.response.use(r=>r,e=>{if(e.response?.status===401&&!e.config?.url?.includes('/auth/')){localStorage.removeItem('token');localStorage.removeItem('role');localStorage.removeItem('fullName');sessionStorage.setItem('message','Votre session a expiré. Reconnectez-vous.');window.location.assign('/login')}return Promise.reject(e)});
export default api;
