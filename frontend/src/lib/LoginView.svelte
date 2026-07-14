<script>
  let { onLoginSuccess, navigate } = $props();

  let username = $state('');
  let password = $state('');
  let error = $state('');
  let loading = $state(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!username.trim() || !password) {
      error = 'Username and password are required';
      return;
    }

    loading = true;
    error = '';

    try {
      const res = await fetch('/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.trim(), password })
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        onLoginSuccess(data.role, data.userId);
      } else {
        error = data.message || 'Invalid username or password';
      }
    } catch (err) {
      console.error(err);
      error = 'Unable to connect to the server';
    } finally {
      loading = false;
    }
  }
</script>

<div class="flex items-center justify-center min-h-[70vh] px-4 animate-fade">
  <div class="card-glass w-full max-w-[420px] p-8 md:p-10 text-center">
    <div class="mb-8">
      <div class="logo-container justify-center mb-2">
        <span>Twilio SMS</span>
        <div class="logo-dot"></div>
      </div>
      <p class="text-sm text-[var(--text-secondary)]">Sign in to your account</p>
    </div>

    {#if error}
      <div class="error-msg mb-4 text-left">
        {error}
      </div>
    {/if}

    <form onsubmit={handleSubmit}>
      <div class="form-group">
        <label class="label" for="username">Username</label>
        <input
          id="username"
          type="text"
          class="input"
          placeholder="Enter username"
          bind:value={username}
          required
          disabled={loading}
        />
      </div>

      <div class="form-group mb-6">
        <label class="label" for="password">Password</label>
        <input
          id="password"
          type="password"
          class="input"
          placeholder="Enter password"
          bind:value={password}
          required
          disabled={loading}
        />
      </div>

      <button type="submit" class="btn btn-primary w-full py-3" disabled={loading}>
        {#if loading}
          Signing in...
        {:else}
          Sign In
        {/if}
      </button>
    </form>

    <div class="mt-6 text-sm text-[var(--text-secondary)]">
      Don't have an account? 
      <button class="text-[var(--cyan)] hover:underline ml-1 font-semibold" onclick={() => navigate('register')}>
        Register
      </button>
    </div>
  </div>
</div>
