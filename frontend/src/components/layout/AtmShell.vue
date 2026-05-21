<template>
  <div class="app" style="height:100vh">
    <div class="atm-cabinet">
      <div class="atm-screen">
        <div class="row" style="margin-bottom:24px">
          <div class="logo-mark" style="width:28px;height:28px;font-size:15px">€</div>
          <div style="font-family:var(--font-display);font-weight:500;font-size:16px">InHolland · ATM</div>
          <span class="spacer" />
          <span class="badge">#14 · Centrum</span>
        </div>
        <slot />
      </div>
    </div>
  </div>
</template>
