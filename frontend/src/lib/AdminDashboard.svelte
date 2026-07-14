<script>
  import { onMount } from 'svelte';
  import AdminCustomerView from './AdminCustomerView.svelte';

  let { onLogout } = $props();

  let loading = $state(true);
  let error = $state('');

  // Dashboard state variables
  let totalCustomers = $state(0);
  let totalSentSms = $state(0);
  let customers = $state([]);
  let stats = $state([]);

  // Modal states
  let isViewModalOpen = $state(false);
  let selectedCustomer = $state({});
  let editCustomerData = $state({});
  let loadingProfile = $state(false);

  async function fetchDashboard() {
    try {
      const res = await fetch('/admin/dashboard');
      if (res.ok) {
        const data = await res.json();
        totalCustomers = data.totalCustomers || 0;
        totalSentSms = data.totalSentSms || 0;
        customers = data.customers || [];
        stats = data.stats || [];
      } else {
        error = 'Failed to load administrator dashboard data';
        onLogout();
      }
    } catch (err) {
      console.error(err);
      error = 'Server connection error';
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    fetchDashboard();
  });

  async function handleOpenEdit(cust) {
    loadingProfile = true;
    editCustomerData = {};
    isViewModalOpen = true;

    try {
      const res = await fetch(`/admin/customer?id=${cust.id}`);
      if (res.ok) {
        const data = await res.json();
        if (data.status === 'success') {
          editCustomerData = { ...data.custProfile, id: cust.id };
        } else {
          alert('Failed to retrieve customer configuration profile');
          isViewModalOpen = false;
        }
      } else {
        alert('Failed to load profile details');
        isViewModalOpen = false;
      }
    } catch (err) {
      console.error(err);
      alert('Network communication failure');
      isViewModalOpen = false;
    } finally {
      loadingProfile = false;
    }
  }

  function handleOpenCreate() {
    editCustomerData = {}; // Empty object denotes CREATE mode
    isViewModalOpen = true;
  }

  async function handleDelete(customerId) {
    if (!confirm('Are you sure you want to permanently delete this customer?')) return;

    try {
      const res = await fetch('/admin/customer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ actionType: 'delete', customerId })
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        fetchDashboard();
      } else {
        alert(data.message || 'Failed to delete customer');
      }
    } catch (err) {
      console.error(err);
      alert('Server connection error');
    }
  }

  async function handleLogout() {
    try {
      await fetch('/logout');
    } catch (err) {
      console.error(err);
    }
    onLogout();
  }

  // Find outbound SMS count for a customer id from stats array
  function getSmsCount(custId) {
    const stat = stats.find(s => s.userId === custId);
    return stat ? stat.sentCount : 0;
  }
</script>

<div class="app-canvas min-h-screen">
  <div class="container flex-grow flex flex-col">
    <!-- Header -->
    <header class="nav-bar">
      <div class="logo-container">
        <span>Admin Console</span>
        <div class="logo-dot"></div>
      </div>
      
      <div class="flex items-center gap-3">
        <button class="btn btn-secondary" onclick={handleOpenCreate}>
          + Create Customer
        </button>
        <button class="btn btn-danger" onclick={handleLogout}>
          Logout
        </button>
      </div>
    </header>

    {#if loading}
      <div class="empty-state">
        <div class="animate-pulse">Loading administrative analytics dashboard...</div>
      </div>
    {:else if error}
      <div class="error-msg my-8 max-w-md mx-auto">
        {error}
      </div>
    {:else}
      <!-- Metric Cards -->
      <div class="metrics-grid">
        <div class="card-glass metric-card">
          <span class="metric-title">Active Accounts</span>
          <span class="metric-val text-gradient font-bold">{totalCustomers}</span>
        </div>
        <div class="card-glass metric-card">
          <span class="metric-title">Total Outbound SMS</span>
          <span class="metric-val text-gradient font-bold">{totalSentSms}</span>
        </div>
      </div>

      <!-- Customer Directory -->
      <div class="card-glass p-6 text-left mb-6">
        <h2 class="text-xl font-bold text-white mb-4">Customer Directory</h2>
        
        {#if customers.length === 0}
          <div class="text-center p-8 text-[var(--text-muted)]">
            No customer accounts registered. Click "+ Create Customer" to register one.
          </div>
        {:else}
          <div class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Full Name</th>
                  <th>Mobile (MSISDN)</th>
                  <th>Email</th>
                  <th>Job Title</th>
                  <th>Outbound SMS</th>
                  <th>Created At</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {#each customers as cust}
                  <tr>
                    <td class="font-semibold text-white">{cust.username}</td>
                    <td>{cust.fullName}</td>
                    <td class="font-mono text-[var(--cyan)]">{cust.msisdn}</td>
                    <td>{cust.email}</td>
                    <td class="text-sm text-[var(--text-secondary)]">{cust.job || '-'}</td>
                    <td class="font-mono font-bold text-[var(--emerald)]">{getSmsCount(cust.id)}</td>
                    <td class="text-xs text-[var(--text-muted)]">
                      {new Date(cust.createdAt).toLocaleDateString()}
                    </td>
                    <td>
                      <div class="flex gap-2">
                        <button class="btn btn-secondary px-3 py-1 text-xs" onclick={() => handleOpenEdit(cust)}>
                          Edit
                        </button>
                        <button class="btn btn-danger px-3 py-1 text-xs" onclick={() => handleDelete(cust.id)}>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        {/if}
      </div>
    {/if}
  </div>
</div>

<!-- Modal: Customer Create/Edit -->
<AdminCustomerView
  customer={editCustomerData}
  isOpen={isViewModalOpen && !loadingProfile}
  onClose={() => isViewModalOpen = false}
  onSave={fetchDashboard}
/>

{#if loadingProfile}
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center">
    <div class="text-white font-bold animate-pulse">Loading Customer Profile...</div>
  </div>
{/if}
