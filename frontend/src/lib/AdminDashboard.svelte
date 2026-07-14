<script>
  import { onMount } from 'svelte';
  import AdminCustomerView from './AdminCustomerView.svelte';
  import { Users, MessageCircle, UserPlus, LogOut, Pencil, Trash2, Send, Terminal, Wifi, Download } from 'lucide-svelte';

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

  // SMPP logs
  let showSmppLogs = $state(false);
  let smppLogs = $state([]);
  let smppLogError = $state('');

  async function fetchSmppLogs() {
    if (!showSmppLogs) return;
    try {
      const res = await fetch('/admin/smpp-logs');
      if (res.ok) {
        const d = await res.json();
        smppLogs = d.logs || [];
        smppLogError = '';
      }
    } catch (err) {
      smppLogError = 'Failed to fetch SMPP logs';
    }
  }

  function smppTime(ts) {
    return ts.slice(0, 23).replace('T', ' ');
  }

  function smppDetail(detail) {
    if (detail.length > 180) return detail.slice(0, 180) + '…';
    return detail;
  }

  let smppPollTimer;
  function toggleSmppLogs() {
    showSmppLogs = !showSmppLogs;
    if (showSmppLogs) {
      fetchSmppLogs();
      smppPollTimer = setInterval(fetchSmppLogs, 3000);
    } else {
      clearInterval(smppPollTimer);
    }
  }

  function smppLogLevelClass(level) {
    if (level === 'ERROR') return 'text-[var(--red)]';
    if (level === 'WARN') return 'text-[var(--yellow)]';
    return 'text-[var(--emerald)]';
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

  // Wireshark capture
  let showWireshark = $state(false);
  let wsCapture = $state({ running: false, fileExists: false, durationSec: 0, packetCount: 0, fileSize: 0 });
  let wsPackets = $state([]);
  let wsError = $state('');

  async function wsStart() {
    wsError = '';
    try {
      const res = await fetch('/admin/wireshark/start', { method: 'POST' });
      const d = await res.json();
      if (d.status !== 'success') { wsError = d.message; return; }
      await wsFetchStatus();
    } catch (e) { wsError = 'Failed to start capture'; }
  }

  async function wsStop() {
    wsError = '';
    try {
      const res = await fetch('/admin/wireshark/stop', { method: 'POST' });
      const d = await res.json();
      if (d.status !== 'success') { wsError = d.message; return; }
      wsCapture = { ...wsCapture, running: false, fileSize: d.fileSize || 0, durationSec: d.durationSec || 0 };
      await wsFetchPackets();
    } catch (e) { wsError = 'Failed to stop capture'; }
  }

  async function wsFetchStatus() {
    try {
      const res = await fetch('/admin/wireshark/status');
      const d = await res.json();
      if (d.status === 'success') {
        wsCapture = { running: d.running, fileExists: d.fileExists, durationSec: d.durationSec || 0, packetCount: d.packetCount || 0, fileSize: d.fileSize || 0 };
      }
    } catch (ignored) {}
  }

  async function wsFetchPackets() {
    try {
      const res = await fetch('/admin/wireshark/packets');
      const d = await res.json();
      if (d.status === 'success') wsPackets = d.packets || [];
    } catch (ignored) {}
  }

  function wsDownload() {
    window.open('/admin/wireshark/download', '_blank');
  }

  let wsPollTimer;
  function toggleWireshark() {
    showWireshark = !showWireshark;
    if (showWireshark) {
      wsFetchStatus();
      wsPollTimer = setInterval(() => {
        wsFetchStatus();
        if (!wsCapture.running) wsFetchPackets();
      }, 2000);
    } else {
      clearInterval(wsPollTimer);
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
        <button class="btn btn-secondary" onclick={toggleWireshark}>
          <Wifi size={14} />
          Wireshark
        </button>
        <button class="btn btn-secondary" onclick={toggleSmppLogs}>
          <Terminal size={14} />
          SMPP Logs
        </button>
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
    <div class="card-glass w-full max-w-[900px] p-8 animate-fade max-h-[85vh] flex flex-col" role="dialog" aria-modal="true" aria-label="SMS History">
      <div class="flex justify-between items-center mb-6">
        <h3 class="font-bold text-xl text-white">SMS History — {smsModal.customerName}</h3>
        <button class="text-white/40 hover:text-white text-lg" onclick={() => smsModal.open = false}>✕</button>
      </div>
      <div class="overflow-y-auto flex-grow space-y-5">
        {#if smsModal.outbound.length === 0 && smsModal.inbound.length === 0}
          <div class="text-center text-sm text-[var(--text-muted)] py-12">No SMS messages yet.</div>
        {:else}
          {#each [...smsModal.outbound, ...smsModal.inbound].sort((a, b) => new Date(a.sentAt || a.createdAt) - new Date(b.sentAt || b.createdAt)) as sms}
            <div class="p-5 rounded-xl border {sms.recipient
                ? 'bg-[var(--cyan)]/10 border-[var(--cyan)]/30 border-l-[3px]'
                : 'bg-[var(--emerald)]/10 border-[var(--emerald)]/30 border-l-[3px]'}">
              <div class="flex justify-between text-sm text-[var(--text-muted)] mb-2">
                <span class="font-bold uppercase tracking-wider">{sms.recipient ? 'OUTBOUND' : 'INBOUND'}</span>
                <span>{new Date(sms.sentAt || sms.createdAt).toLocaleString()}</span>
              </div>
              <div class="text-base text-white leading-relaxed">{sms.message}</div>
              <div class="flex gap-4 mt-2 text-sm text-[var(--text-secondary)]">
                {#if sms.recipient}
                  <span>To: <span class="font-mono font-bold text-[var(--cyan)]">{sms.recipient}</span></span>
                {:else}
                  <span>From: <span class="font-mono font-bold text-[var(--emerald)]">{sms.from}</span></span>
                {/if}
                <span class="capitalize">Status: <span class="font-bold {sms.status === 'delivered' ? 'text-[var(--emerald)]' : 'text-[var(--red)]'}">{sms.status}</span></span>
              </div>
            </div>
          {/each}
        {/if}
      </div>
      <div class="flex justify-end border-t border-[var(--border)] pt-5 mt-5">
        <button class="btn btn-secondary px-6 py-2" onclick={() => smsModal.open = false}>Close</button>
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

<!-- Modal: Wireshark Capture -->
{#if showWireshark}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-6" onkeydown={(e) => e.key === 'Escape' && toggleWireshark()}>
    <div class="card-glass w-full max-w-[1100px] p-8 animate-fade max-h-[90vh] flex flex-col" role="dialog" aria-modal="true" aria-label="Wireshark Capture">
      <div class="flex justify-between items-center mb-6">
        <h3 class="font-bold text-xl text-white flex items-center gap-3">
          <Wifi size={22} class="text-[var(--cyan)]" />
          Wireshark Packet Capture
        </h3>
        <button class="text-white/40 hover:text-white text-lg" onclick={toggleWireshark}>✕</button>
      </div>

      {#if wsError}
        <div class="error-msg mb-4">{wsError}</div>
      {/if}

      <!-- Controls -->
      <div class="flex items-center gap-4 mb-4 p-4 bg-black/30 rounded-xl">
        <span class="text-sm text-[var(--text-secondary)]">Status:
          <span class="font-bold {wsCapture.running ? 'text-[var(--emerald)]' : 'text-[var(--text-muted)]'}">{wsCapture.running ? 'Capturing' : 'Stopped'}</span>
        </span>
        {#if wsCapture.durationSec > 0}
          <span class="text-sm text-[var(--text-secondary)]">Duration: <span class="font-bold text-white">{wsCapture.durationSec}s</span></span>
        {/if}
        <span class="text-sm text-[var(--text-secondary)]">Packets: <span class="font-bold text-white">{wsCapture.packetCount}</span></span>
        {#if wsCapture.fileSize > 0}
          <span class="text-sm text-[var(--text-secondary)]">File: <span class="font-bold text-white">{(wsCapture.fileSize / 1024).toFixed(1)}KB</span></span>
        {/if}

        <div class="flex gap-2 ml-auto">
          {#if !wsCapture.running}
            <button class="btn btn-primary px-4" onclick={wsStart}>
              <Wifi size={14} /> Start Capture
            </button>
          {:else}
            <button class="btn btn-danger px-4" onclick={wsStop}>
              <Wifi size={14} /> Stop
            </button>
          {/if}
          <button class="btn btn-secondary px-4" onclick={wsDownload} disabled={!wsCapture.fileExists}>
            <Download size={14} /> PCAP
          </button>
        </div>
      </div>

      <!-- Packet table -->
      <div class="overflow-y-auto flex-grow bg-black/30 rounded-xl p-4 font-mono text-xs">
        {#if wsPackets.length === 0}
          <div class="text-center text-base text-[var(--text-muted)] py-12">
            {wsCapture.running ? 'Capturing… packets will appear when stopped.' : 'No packets captured. Start a capture and send SMPP traffic.'}
          </div>
        {:else}
          <table class="w-full">
            <thead>
              <tr class="text-[var(--text-muted)] uppercase tracking-wider text-left">
                <th class="p-2">Time</th>
                <th class="p-2">Src</th>
                <th class="p-2">Dst</th>
                <th class="p-2">Command</th>
                <th class="p-2">Src/Dst Addr</th>
                <th class="p-2">Message</th>
              </tr>
            </thead>
            <tbody>
              {#each wsPackets as pkt}
                <tr class="border-b border-white/5 hover:bg-white/[0.03]">
                  <td class="p-2 text-[var(--text-muted)]">{pkt.time ? pkt.time.toFixed(3) : ''}</td>
                  <td class="p-2 text-white">{pkt.src || ''}</td>
                  <td class="p-2 text-white">{pkt.dst || ''}</td>
                  <td class="p-2 text-[var(--cyan)] font-semibold">{pkt.cmd || pkt.cmdRaw || ''}</td>
                  <td class="p-2 text-[var(--text-secondary)]">{pkt.srcAddr || ''}{pkt.dstAddr ? ` → ${pkt.dstAddr}` : ''}</td>
                  <td class="p-2 text-white/80 max-w-[200px] truncate">{pkt.message || ''}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        {/if}
      </div>

      <div class="flex justify-between items-center border-t border-[var(--border)] pt-5 mt-5">
        <span class="text-sm text-[var(--text-muted)]">Auto-refreshes every 2s · {wsPackets.length} packets</span>
        <button class="btn btn-secondary px-6" onclick={toggleWireshark}>Close</button>
      </div>
    </div>
  </div>
{/if}

<!-- Modal: SMPP Logs -->
{#if showSmppLogs}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-6" onkeydown={(e) => e.key === 'Escape' && toggleSmppLogs()}>
    <div class="card-glass w-full max-w-[1000px] p-8 animate-fade max-h-[85vh] flex flex-col" role="dialog" aria-modal="true" aria-label="SMPP Logs">
      <div class="flex justify-between items-center mb-6">
        <h3 class="font-bold text-xl text-white flex items-center gap-3">
          <Terminal size={22} class="text-[var(--cyan)]" />
          SMPP Session Logs
        </h3>
        <button class="text-white/40 hover:text-white text-lg" onclick={toggleSmppLogs}>✕</button>
      </div>

      {#if smppLogError}
        <div class="error-msg mb-4">{smppLogError}</div>
      {/if}

      <div class="overflow-y-auto flex-grow space-y-2 bg-black/30 rounded-xl p-5 font-mono text-sm">
        {#if smppLogs.length === 0}
          <div class="text-center text-base text-[var(--text-muted)] py-12">No SMPP events yet. Send an SMS or trigger MO.</div>
        {:else}
          {#each smppLogs as log}
            <div class="flex gap-3 py-2 px-3 border-b border-white/5 last:border-0 hover:bg-white/[0.03] rounded items-start">
              <span class="text-[var(--text-muted)] w-[22ch] shrink-0 text-xs leading-5">{smppTime(log.timestamp)}</span>
              <span class="w-[7ch] shrink-0 font-bold uppercase text-sm {smppLogLevelClass(log.level)}">{log.level}</span>
              <span class="w-[9ch] shrink-0 text-[var(--cyan)] font-semibold text-sm">{log.event}</span>
              <span class="text-white/80 leading-5 break-words min-w-0">{smppDetail(log.detail)}</span>
            </div>
          {/each}
        {/if}
      </div>
      <div class="flex justify-between items-center border-t border-[var(--border)] pt-5 mt-5">
        <span class="text-sm text-[var(--text-muted)]">Auto-refreshes every 3s · {smppLogs.length} entries</span>
        <button class="btn btn-secondary px-6" onclick={toggleSmppLogs}>Close</button>
      </div>
    </div>
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
