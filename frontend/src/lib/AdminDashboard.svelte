<script>
  import { onMount } from 'svelte';
  import AdminCustomerView from './AdminCustomerView.svelte';
  import { Users, MessageCircle, UserPlus, LogOut, Pencil, Trash2, Send } from 'lucide-svelte';

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
    fetchBroadcasts();
    const iv = setInterval(fetchBroadcasts, 30000);
    return () => clearInterval(iv);
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

  // Broadcast history
  let broadcasts = $state([]);

  async function fetchBroadcasts() {
    try {
      const res = await fetch('/api/chat/system?limit=100');
      if (res.ok) {
        const d = await res.json();
        broadcasts = (d.messages || []).reverse();
      }
    } catch (ignored) {}
  }

  // SMS history modal
  let smsModal = $state({ open: false, customerName: '', outbound: [], inbound: [] });

  async function openSmsHistory(cust) {
    try {
      const res = await fetch(`/admin/customer?id=${cust.id}&action=sms_history`);
      if (res.ok) {
        const d = await res.json();
        if (d.status === 'success') {
          smsModal = {
            open: true,
            customerName: cust.fullName || cust.username,
            outbound: d.outboundHistory || [],
            inbound: d.inboundHistory || []
          };
        }
      }
    } catch (ignored) {}
  }

  // Broadcast modal
  let showBroadcast = $state(false);
  let broadcastMsg = $state('');
  let broadcastSms = $state(false);
  let broadcastSending = $state(false);
  let broadcastResult = $state('');

  async function handleBroadcast(e) {
    e.preventDefault();
    if (!broadcastMsg.trim()) return;
    broadcastSending = true;
    broadcastResult = '';
    try {
      const res = await fetch('/admin/broadcast', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: broadcastMsg.trim(), sendSms: broadcastSms })
      });
      const d = await res.json();
      if (res.ok && d.status === 'success') {
        broadcastResult = `Sent to ${d.pushedCount} users`;
        broadcastMsg = '';
        setTimeout(() => showBroadcast = false, 2000);
      } else {
        broadcastResult = d.message || 'Broadcast failed';
      }
    } catch (err) {
      broadcastResult = 'Server error';
    } finally {
      broadcastSending = false;
    }
  }
</script>

<div class="app-canvas min-h-screen">
  <div class="container flex-grow flex flex-col">
    <!-- Header -->
    <header class="nav-bar">
      <div class="logo-container">
        <Users size={22} class="text-[var(--cyan)]" />
        <span>Admin Console</span>
        <div class="logo-dot"></div>
      </div>
      
      <div class="flex items-center gap-3">
        <button class="btn btn-secondary" onclick={() => showBroadcast = true}>
          <Send size={14} />
          Broadcast
        </button>
        <button class="btn btn-secondary" onclick={handleOpenCreate}>
          <UserPlus size={16} />
          Create Customer
        </button>
        <button class="btn btn-danger" onclick={handleLogout}>
          <LogOut size={14} />
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
          <div class="flex items-center gap-2">
            <Users size={18} class="text-[var(--cyan)]" />
            <span class="metric-title">Active Accounts</span>
          </div>
          <span class="metric-val text-gradient font-bold">{totalCustomers}</span>
        </div>
        <div class="card-glass metric-card">
          <div class="flex items-center gap-2">
            <MessageCircle size={18} class="text-[var(--emerald)]" />
            <span class="metric-title">Total Outbound SMS</span>
          </div>
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
                            <Pencil size={12} />
                            Edit
                          </button>
                          <button class="btn btn-secondary px-3 py-1 text-xs" onclick={() => openSmsHistory(cust)}>
                            <MessageCircle size={12} />
                            SMS
                          </button>
                          <button class="btn btn-danger px-3 py-1 text-xs" onclick={() => handleDelete(cust.id)}>
                            <Trash2 size={12} />
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

      <!-- Broadcasts Section -->
      <div class="card-glass p-6 text-left mb-6">
        <h2 class="text-xl font-bold text-white mb-4">Broadcasts</h2>
        <div class="flex-grow overflow-y-auto flex flex-col gap-3 max-h-[400px] bg-black/20 rounded-lg p-4">
          {#if broadcasts.length === 0}
            <div class="text-center text-sm text-[var(--text-muted)]">No broadcasts sent yet.</div>
          {:else}
            {#each broadcasts as msg}
              <div class="message-bubble inbound">
                <div class="msg-text"><span class="font-bold text-[var(--yellow)]">📢 Broadcast:</span> {msg.content}</div>
                <div class="msg-meta">
                  <span>{new Date(msg.createdAt).toLocaleString()}</span>
                </div>
              </div>
            {/each}
          {/if}
        </div>
      </div>
    {/if}
  </div>
</div>

<!-- SMS History Modal -->
{#if smsModal.open}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4" onkeydown={(e) => e.key === 'Escape' && (smsModal.open = false)}>
    <div class="card-glass w-full max-w-[700px] p-6 animate-fade max-h-[80vh] flex flex-col" role="dialog" aria-modal="true" aria-label="SMS History">
      <div class="flex justify-between items-center mb-4">
        <h3 class="font-bold text-lg text-white">SMS History — {smsModal.customerName}</h3>
        <button class="text-white/40 hover:text-white" onclick={() => smsModal.open = false}>✕</button>
      </div>
      <div class="overflow-y-auto flex-grow space-y-3">
        {#if smsModal.outbound.length === 0 && smsModal.inbound.length === 0}
          <div class="text-center text-sm text-[var(--text-muted)] py-8">No SMS messages yet.</div>
        {:else}
          {#each [...smsModal.outbound, ...smsModal.inbound].sort((a, b) => new Date(a.sentAt || a.createdAt) - new Date(b.sentAt || b.createdAt)) as sms}
            <div class="p-3 rounded-lg {sms.recipient ? 'bg-[var(--cyan)]/10 border-l-2 border-[var(--cyan)]' : 'bg-[var(--emerald)]/10 border-l-2 border-[var(--emerald)]'}">
              <div class="flex justify-between text-xs text-[var(--text-muted)] mb-1">
                <span class="font-bold uppercase">{sms.recipient ? 'OUTBOUND' : 'INBOUND'}</span>
                <span>{new Date(sms.sentAt || sms.createdAt).toLocaleString()}</span>
              </div>
              <div class="text-sm text-white">{sms.message}</div>
              <div class="flex gap-3 mt-1 text-xs text-[var(--text-secondary)]">
                {#if sms.recipient}
                  <span>To: <span class="font-mono">{sms.recipient}</span></span>
                {:else}
                  <span>From: <span class="font-mono">{sms.from}</span></span>
                {/if}
                <span class="capitalize">Status: <span class="font-bold {sms.status === 'delivered' ? 'text-[var(--emerald)]' : 'text-[var(--red)]'}">{sms.status}</span></span>
              </div>
            </div>
          {/each}
        {/if}
      </div>
      <div class="flex justify-end border-t border-[var(--border)] pt-4 mt-4">
        <button class="btn btn-secondary px-4" onclick={() => smsModal.open = false}>Close</button>
      </div>
    </div>
  </div>
{/if}

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

<!-- Modal: Broadcast -->
{#if showBroadcast}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4" onkeydown={(e) => e.key === 'Escape' && (showBroadcast = false)}>
    <div class="card-glass w-full max-w-[500px] p-6 animate-fade" role="dialog" aria-modal="true" aria-labelledby="broadcast-title">
      <div class="flex justify-between items-center mb-6">
        <h3 id="broadcast-title" class="font-bold text-lg text-white">Send Broadcast</h3>
        <button class="text-white/40 hover:text-white" onclick={() => showBroadcast = false}>✕</button>
      </div>

      <form onsubmit={handleBroadcast}>
        <div class="form-group mb-4">
          <label class="label" for="broadcastMsg">Message</label>
          <textarea
            id="broadcastMsg"
            class="input w-full min-h-[100px]"
            placeholder="Type your broadcast message..."
            bind:value={broadcastMsg}
            required
          ></textarea>
        </div>

        <div class="form-group mb-6">
          <label class="flex items-center gap-2 cursor-pointer text-sm text-[var(--text-secondary)]">
            <input type="checkbox" bind:checked={broadcastSms} class="accent-[var(--cyan)]" />
            Also send as real SMS (via user's configured provider)
          </label>
        </div>

        {#if broadcastResult}
          <div class="text-sm mb-4 p-2 rounded {broadcastResult.includes('failed') || broadcastResult.includes('error') ? 'text-[var(--red)] bg-red-500/10' : 'text-[var(--emerald)] bg-emerald-500/10'}">
            {broadcastResult}
          </div>
        {/if}

        <div class="flex gap-3 justify-end border-t border-[var(--border)] pt-4">
          <button type="button" class="btn btn-secondary" onclick={() => showBroadcast = false}>Cancel</button>
          <button type="submit" class="btn btn-primary px-6" disabled={broadcastSending || !broadcastMsg.trim()}>
            {broadcastSending ? 'Sending...' : 'Send Broadcast'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}
