import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const OAuth2CallbackPage = () => {
  const [searchParams] = useSearchParams();
  const { login } = useAuth();

  useEffect(() => {
    const token = searchParams.get('token');
    const id = searchParams.get('id');
    const name = searchParams.get('name');
    const email = searchParams.get('email');
    const barangay = searchParams.get('barangay');
    const role = searchParams.get('role');
    const reputationScore = searchParams.get('reputationScore');

    if (token) {
      const user = {
        id: id ? parseInt(id, 10) : null,
        name,
        email,
        barangay,
        role,
        reputationScore: reputationScore ? parseInt(reputationScore, 10) : 0,
      };
      login(user, token);

      if (!barangay || barangay === 'Not set') {
        window.location.href = '/select-barangay';
      } else {
        window.location.href = '/dashboard';
      }
    } else {
      window.location.href = '/login';
    }
  }, [login, searchParams]);

  return null;
};

export default OAuth2CallbackPage;