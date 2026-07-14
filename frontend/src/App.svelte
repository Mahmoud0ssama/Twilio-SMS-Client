<script>
  import { onMount } from 'svelte';
  import LoginView from './lib/LoginView.svelte';
  import RegisterView from './lib/RegisterView.svelte';
  import VerifyMsisdnView from './lib/VerifyMsisdnView.svelte';
  import CustomerDashboard from './lib/CustomerDashboard.svelte';
  import AdminDashboard from './lib/AdminDashboard.svelte';

  // SPA Route State
  let currentView = $state('login'); // login, register, verify-msisdn, customer-dashboard, admin-dashboard
  let userRole = $state('');
  let userId = $state(null);
  let verifyingSession = $state(true);

  async function checkSession() {
    try {
      // Fetch profile to see if session is active
      const res = await fetch('/profile');
      if (res.ok) {
        const data = await res.json();
        // Since we are authenticated, determine role
        // Wait, does /profile return role? Let's check:
        // Actually, let's fetch /dashboard to see if we can login as customer, 
        // or check /admin/dashboard to see if we are admin.
        // Even simpler: we can try to fetch /dashboard first. If it succeeds, it's a customer.
        // If it fails with 403, we can check if we can access /admin/dashboard.
        
        const dashRes = await fetch('/dashboard');
        if (dashRes.ok) {
          userRole = 'customer';
          currentView = 'customer-dashboard';
        } else {
          const adminRes = await fetch('/admin/dashboard');
          if (adminRes.ok) {
            userRole = 'administrator';
            currentView = 'admin-dashboard';
          } else {
            currentView = 'login';
          }
        }
      } else {
        // Not authenticated
        currentView = 'login';
      }
    } catch (err) {
      console.error(err);
      currentView = 'login';
    } finally {
      verifyingSession = false;
    }
  }

  onMount(() => {
    checkSession();
  });

  function handleLoginSuccess(role, id) {
    userRole = role;
    userId = id;
    if (role === 'administrator') {
      currentView = 'admin-dashboard';
    } else {
      currentView = 'customer-dashboard';
    }
  }

  function handleLogout() {
    userRole = '';
    userId = null;
    currentView = 'login';
  }

  function navigate(view) {
    currentView = view;
  }
</script>

<main class="app-canvas min-h-screen">
  {#if verifyingSession}
    <div class="flex items-center justify-center min-h-screen">
      <div class="text-white font-bold animate-pulse">Verifying secure session...</div>
    </div>
  {:else}
    {#if currentView === 'login'}
      <LoginView onLoginSuccess={handleLoginSuccess} {navigate} />
    {:else if currentView === 'register'}
      <RegisterView {navigate} />
    {:else if currentView === 'verify-msisdn'}
      <VerifyMsisdnView {navigate} />
    {:else if currentView === 'customer-dashboard'}
      <CustomerDashboard onLogout={handleLogout} />
    {:else if currentView === 'admin-dashboard'}
      <AdminDashboard onLogout={handleLogout} />
    {/if}
  {/if}
</main>
