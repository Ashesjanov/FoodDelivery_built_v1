<template>
  <main class="min-h-screen bg-zinc-50 px-4 py-8 md:px-8" id="main-content">
    <div class="mx-auto flex min-h-[calc(100vh-4rem)] max-w-6xl flex-col justify-center gap-6">
      <RouterLink to="/home" class="w-fit rounded-xl px-2 py-1 text-lg font-semibold text-zinc-950 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500">
        食刻送达
      </RouterLink>
      <div class="grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <section class="flex min-h-72 min-w-0 flex-col justify-between bg-orange-50 p-6 md:p-8">
          <div>
            <p class="text-sm text-orange-700">热饭、好味、准时送达</p>
            <h1 class="mt-4 max-w-xl text-3xl font-semibold text-zinc-950 md:text-4xl">把每一餐安排得更轻松</h1>
          </div>
          <p class="max-w-md text-sm leading-6 text-zinc-700">顾客、商家、骑手和运营团队在同一个清晰工作台里协作。</p>
        </section>
        <section class="min-w-0">
          <RouterView />
        </section>
      </div>
    </div>
  </main>
</template>
