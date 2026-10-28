/*
 * 매출 데이터 올리기 모달 (F17)
 * 1 올리기(엑셀/CSV · 영수증 사진 · 직접 입력) → 2 결과 확인(문제 있는 줄 수정) → 3 분석 완료
 * 페이지에서 PRODUCTS = [{id, name, price}] 를 먼저 정의해야 한다.
 */
(() => {
  const modal = document.getElementById("sales-upload-modal");
  if (!modal) return;
  const $ = (sel) => modal.querySelector(sel);
  const $$ = (sel) => Array.from(modal.querySelectorAll(sel));
  const A = (f) => `/assets/${f}`;

  const state = {
    step: 1,
    tab: "excel",
    file: null,
    receipt: null, // {file, preview, rows|null}
    manual: [], // {id, date, productId, quantity, amount, amountTouched}
    rows: [], // {id, date, menuName, productId, quantity, amount, editing}
    rowsSource: "",
    busy: null, // "parse" | "analyze" | null
    committed: 0,
    dirtyCommit: false,
  };

  const uid = () => Math.random().toString(36).slice(2, 9);
  const pad = (n) => String(n).padStart(2, "0");
  const todayStr = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
  const validDate = (s) => /^\d{4}-\d{1,2}-\d{1,2}$/.test(s || "") && !Number.isNaN(new Date(s).getTime());
  const shortDate = (s) => (validDate(s) ? `${new Date(s).getMonth() + 1}/${new Date(s).getDate()}` : s || "-");
  const longDate = (t) => `${new Date(t).getMonth() + 1}월 ${new Date(t).getDate()}일`;
  const digits = (v) => { const d = String(v ?? "").replace(/[^0-9]/g, ""); return d ? Number(d) : null; };
  const productName = (id) => (PRODUCTS.find((p) => p.id === id) || {}).name;
  const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  const issueOf = (r) => {
    if (!validDate(r.date)) return "날짜 확인 필요";
    if (!r.productId) return "상품 확인 필요";
    if (!r.quantity) return "수량 확인 필요";
    if (r.amount === null || r.amount === undefined) return "금액 확인 필요";
    return null;
  };
  const manualValid = () => state.manual.filter((r) => r.productId && Number(r.quantity) > 0);
  const sourceKey = () =>
    state.tab === "excel" ? `excel:${state.file?.name}:${state.file?.size}:${state.file?.lastModified}`
      : state.tab === "receipt" ? `receipt:${state.receipt?.preview}`
        : `manual:${JSON.stringify(manualValid())}`;
  const periodText = () => {
    const times = state.rows.filter((r) => validDate(r.date)).map((r) => new Date(r.date).getTime());
    return times.length ? `분석 기간 ${longDate(Math.min(...times))} ~ ${longDate(Math.max(...times))}` : "분석 기간 -";
  };

  /* ---------- 화면 그리기 ---------- */
  function render() {
    // 단계 표시
    $$("[data-su-step]").forEach((el) => {
      const n = Number(el.dataset.suStep);
      const on = state.step >= n;
      const dot = el.querySelector("[data-su-dot]");
      dot.classList.toggle("border-[#2563eb]", on); dot.classList.toggle("bg-[#2563eb]", on);
      dot.classList.toggle("border-[#e5eaf1]", !on); dot.classList.toggle("bg-[#f0f3f8]", !on);
      const num = el.querySelector("[data-su-num]");
      num.textContent = state.step > n ? "✓" : n;
      num.classList.toggle("text-white", on); num.classList.toggle("text-[#98a2b3]", !on);
      const label = el.querySelector("[data-su-label]");
      label.classList.toggle("text-[#2563eb]", on); label.classList.toggle("text-[#98a2b3]", !on);
      label.classList.toggle("f-bold", state.step === n); label.classList.toggle("f-medium", state.step !== n);
    });
    $$("[data-su-line]").forEach((el) => {
      const on = state.step > Number(el.dataset.suLine);
      el.classList.toggle("bg-[#2563eb]", on); el.classList.toggle("bg-[#e5eaf1]", !on);
    });
    $$("[data-su-panel]").forEach((p) => (p.hidden = Number(p.dataset.suPanel) !== state.step));

    // 탭
    $$("[data-su-tab]").forEach((b) => {
      const on = b.dataset.suTab === state.tab;
      b.setAttribute("aria-selected", String(on));
      ["f-bold", "border", "border-[#e5eaf1]", "bg-white", "text-[#2563eb]"].forEach((c) => b.classList.toggle(c, on));
      ["f-medium", "text-[#667085]", "hover:text-[#172033]"].forEach((c) => b.classList.toggle(c, !on));
    });
    $$("[data-su-tab-panel]").forEach((p) => (p.hidden = p.dataset.suTabPanel !== state.tab));

    // 선택한 파일
    const excelZone = $('[data-su-tab-panel="excel"] [data-su-drop]');
    setSelected(excelZone, state.file && { name: state.file.name, meta: `${(state.file.size / 1024).toFixed(1)}KB · 다음을 누르면 내용을 확인해요` });
    const receiptZone = $('[data-su-tab-panel="receipt"] [data-su-drop]');
    const recognizing = state.receipt && !state.receipt.rows;
    receiptZone.hidden = recognizing;
    $("[data-su-receipt-loading]").hidden = !recognizing;
    setSelected(receiptZone, state.receipt?.rows && { name: state.receipt.file.name, meta: `${state.receipt.rows.length}건 인식했어요 · 다음을 누르면 내용을 확인해요`, preview: state.receipt.preview });

    if (state.step === 2) renderCheck();
    if (state.step === 3) {
      $("[data-su-done]").textContent = `${state.committed}건의 매출 데이터를 반영했어요.`;
      $("[data-su-done-period]").textContent = periodText();
    }

    // 하단 버튼
    const back = $("[data-su-back]");
    const next = $("[data-su-next]");
    back.textContent = state.step === 1 ? "취소" : state.step === 2 ? "이전" : "닫기";
    back.disabled = state.busy !== null;
    $("[data-su-close]").disabled = state.busy !== null;
    if (state.step === 1) {
      next.textContent = state.busy === "parse" ? "불러오는 중..." : "다음";
      next.disabled = state.busy !== null || !(state.tab === "excel" ? state.file : state.tab === "receipt" ? state.receipt?.rows : manualValid().length);
    } else if (state.step === 2) {
      next.textContent = "분석 시작";
      next.disabled = state.busy !== null || state.rows.some(issueOf) || !state.rows.length;
    } else {
      next.textContent = "성과 분석 보기";
      next.disabled = false;
    }
  }

  function setSelected(zone, info) {
    zone.querySelector("[data-su-empty]").hidden = !!info;
    zone.querySelector("[data-su-selected]").hidden = !info;
    if (!info) return;
    zone.querySelector("[data-su-name]").textContent = info.name;
    zone.querySelector("[data-su-meta]").textContent = info.meta;
    const img = zone.querySelector("[data-su-preview]");
    if (img) img.src = info.preview || "";
  }

  /* ---------- 직접 입력 ---------- */
  const manualInput = "f-regular h-[40px] w-full min-w-0 rounded-[10px] border border-[#d5dce7] bg-white px-[10px] text-[14px] text-[#172033] outline-none focus:border-[#2563eb]";
  function renderManual() {
    const box = $("[data-su-manual-rows]");
    box.querySelectorAll("[data-row]").forEach((el) => el.remove());
    $("[data-su-manual-empty]").hidden = state.manual.length > 0;
    state.manual.forEach((r) => {
      const row = document.createElement("div");
      row.dataset.row = r.id;
      row.className = "flex h-[56px] w-full items-center gap-[12px] border-b border-[#eef1f5] px-[16px]";
      row.innerHTML = `
        <input type="date" data-k="date" aria-label="날짜" class="${manualInput} w-[160px] shrink-0">
        <span class="relative flex min-w-0 flex-1 items-center">
          <select data-k="productId" aria-label="상품명" class="${manualInput} appearance-none pr-[32px]">
            ${PRODUCTS.map((p) => `<option value="${p.id}">${esc(p.name)}</option>`).join("")}
          </select>
          <img alt="" src="${A("22c5d.svg")}" class="pointer-events-none absolute right-[10px] size-[16px]">
        </span>
        <input inputmode="numeric" data-k="quantity" aria-label="수량" class="${manualInput} w-[90px] shrink-0">
        <input inputmode="numeric" data-k="amount" aria-label="금액" placeholder="금액" class="${manualInput} w-[150px] shrink-0">
        <button type="button" data-remove aria-label="행 삭제" class="flex size-[40px] shrink-0 items-center justify-center rounded-[10px] hover:bg-[#f7f9fc]">
          <img alt="" src="${A("b3b18.svg")}" class="size-[16px]">
        </button>`;
      row.querySelector('[data-k="date"]').value = r.date;
      row.querySelector('[data-k="productId"]').value = String(r.productId);
      row.querySelector('[data-k="quantity"]').value = r.quantity;
      row.querySelector('[data-k="amount"]').value = r.amount;
      box.appendChild(row);
    });
  }
  // 금액을 직접 고치지 않았다면 상품·수량이 바뀔 때 가격 × 수량으로 다시 계산한다.
  function updateManual(id, key, value) {
    const r = state.manual.find((x) => x.id === id);
    if (!r) return;
    if (key === "productId") r.productId = Number(value);
    else if (key === "quantity") r.quantity = String(value).replace(/[^0-9]/g, "");
    else if (key === "amount") { r.amount = String(value).replace(/[^0-9]/g, ""); r.amountTouched = true; }
    else r[key] = value;
    if (!r.amountTouched && (key === "productId" || key === "quantity")) {
      const price = (PRODUCTS.find((p) => p.id === r.productId) || {}).price || 0;
      r.amount = price && Number(r.quantity) ? String(price * Number(r.quantity)) : "";
      const amountInput = modal.querySelector(`[data-row="${id}"] [data-k="amount"]`);
      if (amountInput) amountInput.value = r.amount;
    }
  }
  $("[data-su-manual-rows]").addEventListener("input", (e) => {
    const row = e.target.closest("[data-row]");
    if (!row || !e.target.dataset.k) return;
    if (e.target.dataset.k === "quantity" || e.target.dataset.k === "amount") e.target.value = e.target.value.replace(/[^0-9]/g, "");
    updateManual(row.dataset.row, e.target.dataset.k, e.target.value);
    render();
  });
  $("[data-su-manual-rows]").addEventListener("change", (e) => {
    const row = e.target.closest("[data-row]");
    if (row && e.target.dataset.k) { updateManual(row.dataset.row, e.target.dataset.k, e.target.value); render(); }
  });
  $("[data-su-manual-rows]").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-remove]");
    if (!btn) return;
    state.manual = state.manual.filter((r) => r.id !== btn.closest("[data-row]").dataset.row);
    renderManual();
    render();
  });
  $("[data-su-manual-add]").addEventListener("click", () => {
    const first = PRODUCTS[0];
    state.manual.push({
      id: uid(),
      date: state.manual.length ? state.manual[state.manual.length - 1].date : todayStr(),
      productId: first ? first.id : null,
      quantity: "1",
      amount: first ? String(first.price) : "",
      amountTouched: false,
    });
    renderManual();
    render();
  });

  /* ---------- 결과 확인 표 ---------- */
  const cellInput = "f-regular h-[28px] w-full min-w-0 rounded-[6px] border bg-white px-[8px] text-[13px] text-[#172033] outline-none focus:border-[#2563eb]";
  const pencil = (field, label) =>
    `<button type="button" data-edit="${field}" aria-label="${label} 수정" class="shrink-0 rounded-[4px] hover:opacity-60"><img alt="" src="${A("29167.svg")}" class="size-[12px]"></button>`;

  function rowHtml(r) {
    const issue = issueOf(r);
    const edit = r.editing || !!issue;
    const warn = "border-[#eacba5]";
    const ok = "border-[#d5dce7]";
    const dateCell = edit && (r.editing || issue === "날짜 확인 필요")
      ? `<input type="date" data-f="date" value="${validDate(r.date) ? esc(r.date) : ""}" aria-label="날짜" class="${cellInput} px-[4px] ${validDate(r.date) ? ok : warn}">`
      : `<p class="f-regular text-[13px] whitespace-nowrap text-[#667085]">${esc(shortDate(r.date))}</p>${pencil("date", "날짜")}`;
    const menuSelect = edit && (r.editing || !r.productId)
      ? `<label class="relative flex h-[28px] shrink-0 items-center gap-[6px] rounded-[6px] border bg-white px-[8px] ${r.productId ? ok : warn}">
           <span class="f-semibold text-[11px] whitespace-nowrap ${r.productId ? "text-[#667085]" : "text-[#c46b12]"}">상품 선택</span>
           <img alt="" src="${A("a50d1.svg")}" class="size-[12px]">
           <select data-f="productId" aria-label="${esc(r.menuName)} 상품 선택" class="absolute inset-0 cursor-pointer opacity-0">
             <option value="">상품 선택</option>
             ${PRODUCTS.map((p) => `<option value="${p.id}" ${p.id === r.productId ? "selected" : ""}>${esc(p.name)}</option>`).join("")}
           </select>
         </label>`
      : pencil("menu", "상품명");
    const qtyCell = edit && (r.editing || !r.quantity)
      ? `<input inputmode="numeric" data-f="quantity" value="${r.quantity ?? ""}" placeholder="수량" aria-label="수량" class="${cellInput} ${r.quantity ? ok : warn}">`
      : `<p class="f-regular text-[13px] whitespace-nowrap text-[#172033]">${r.quantity}</p>${pencil("quantity", "수량")}`;
    const amountCell = edit && (r.editing || r.amount === null)
      ? `<input inputmode="numeric" data-f="amount" value="${r.amount ?? ""}" placeholder="금액" aria-label="금액" class="${cellInput} ${r.amount !== null ? ok : warn}">`
      : `<p class="f-regular text-[13px] whitespace-nowrap text-[#172033]">${(r.amount ?? 0).toLocaleString()}원</p>${pencil("amount", "금액")}`;
    const status = issue
      ? `<span class="f-semibold rounded-full bg-[#ffedd4] px-[9px] py-[5px] text-[11px] whitespace-nowrap text-[#c46b12]">${issue}</span>`
      : `<span class="flex items-center gap-[4px] rounded-full bg-[#ddf8f3] px-[9px] py-[5px]"><img alt="" src="${A("32c90.svg")}" class="size-[12px]"><span class="f-semibold text-[11px] whitespace-nowrap text-[#0d9488]">확인됨</span></span>`;
    const done = r.editing && !issue ? `<button type="button" data-done class="f-semibold text-[11px] text-[#2563eb] hover:underline">완료</button>` : "";
    return `
      <div data-check-row="${r.id}" class="flex h-[46px] w-full items-center gap-[16px] border-b border-[#eef1f5] px-[16px] ${issue ? "bg-[#fff5e9]" : "bg-white"}">
        <div class="flex w-[86px] shrink-0 items-center gap-[8px]">${dateCell}</div>
        <div class="flex w-[320px] shrink-0 items-center justify-between gap-[8px]">
          <p class="f-medium min-w-0 truncate text-[14px] text-[#172033]">${esc(productName(r.productId) || r.menuName)}</p>${menuSelect}
        </div>
        <div class="flex w-[80px] shrink-0 items-center gap-[8px]">${qtyCell}</div>
        <div class="flex w-[150px] shrink-0 items-center justify-between gap-[8px]">${amountCell}</div>
        <div class="flex w-[132px] shrink-0 items-center gap-[6px]">${status}${done}</div>
      </div>`;
  }

  function renderCheck() {
    const analyzing = state.busy === "analyze";
    $("[data-su-analyzing]").hidden = !analyzing;
    $("[data-su-check]").hidden = analyzing;
    const issues = state.rows.filter(issueOf).length;
    $("[data-su-total]").textContent = `총 ${state.rows.length}건 인식`;
    const badge = $("[data-su-issues]");
    badge.textContent = `확인 필요 ${issues}건`;
    badge.className = `f-semibold rounded-full px-[12px] py-[7px] text-[13px] ${issues ? "bg-[#fff5e9] text-[#c46b12]" : "bg-[#ddf8f3] text-[#0d9488]"}`;
    $("[data-su-period]").textContent = periodText();
    updateVisibleCount();
  }

  function renderRows() {
    $("[data-su-rows]").innerHTML = state.rows.map(rowHtml).join("");
  }

  function rerenderRow(id) {
    const r = state.rows.find((x) => x.id === id);
    const old = modal.querySelector(`[data-check-row="${id}"]`);
    const focusField = document.activeElement && old.contains(document.activeElement) ? document.activeElement.dataset.f : null;
    old.outerHTML = rowHtml(r);
    if (focusField) {
      const el = modal.querySelector(`[data-check-row="${id}"] [data-f="${focusField}"]`);
      if (el) { el.focus(); if (el.setSelectionRange && el.type !== "date") el.setSelectionRange(el.value.length, el.value.length); }
    }
    renderCheck();
    render();
  }

  function updateVisibleCount() {
    const box = $("[data-su-rows]");
    const n = state.rows.length;
    const first = n ? Math.min(n, Math.floor(box.scrollTop / 46) + 1) : 0;
    const last = Math.min(n, first + 6);
    $("[data-su-visible]").textContent = `${n}건 중 ${first}–${last}건 표시`;
  }
  $("[data-su-rows]").addEventListener("scroll", updateVisibleCount);

  $("[data-su-rows]").addEventListener("click", (e) => {
    const rowEl = e.target.closest("[data-check-row]");
    if (!rowEl) return;
    const r = state.rows.find((x) => x.id === rowEl.dataset.checkRow);
    if (e.target.closest("[data-edit]")) { r.editing = true; rerenderRow(r.id); }
    if (e.target.closest("[data-done]")) { r.editing = false; rerenderRow(r.id); }
  });
  // 입력 중에는 값만 반영하고, 칸을 벗어나거나 선택을 바꾸면 줄을 다시 그린다.
  $("[data-su-rows]").addEventListener("input", (e) => {
    const rowEl = e.target.closest("[data-check-row]");
    if (!rowEl || !e.target.dataset.f) return;
    const r = state.rows.find((x) => x.id === rowEl.dataset.checkRow);
    const f = e.target.dataset.f;
    if (f === "quantity" || f === "amount") { e.target.value = e.target.value.replace(/[^0-9]/g, ""); r[f] = digits(e.target.value); }
    renderCheck();
    render();
  });
  $("[data-su-rows]").addEventListener("change", (e) => {
    const rowEl = e.target.closest("[data-check-row]");
    if (!rowEl || !e.target.dataset.f) return;
    const r = state.rows.find((x) => x.id === rowEl.dataset.checkRow);
    const f = e.target.dataset.f;
    if (f === "date") r.date = e.target.value;
    else if (f === "productId") r.productId = e.target.value ? Number(e.target.value) : null;
    else r[f] = digits(e.target.value);
    rerenderRow(r.id);
  });

  /* ---------- 파일 선택 ---------- */
  async function onExcel(file) {
    state.file = file;
    render();
  }
  async function onReceipt(file) {
    const preview = URL.createObjectURL(file);
    state.receipt = { file, preview, rows: null };
    render();
    const body = new FormData();
    body.append("file", file);
    const [rows] = await Promise.all([
      fetch("/api/sales/receipt", { method: "POST", body }).then((r) => r.json()),
      new Promise((r) => setTimeout(r, 2000)), // 모의 OCR 대기
    ]);
    if (state.receipt?.preview === preview) state.receipt.rows = rows;
    render();
  }
  $$("[data-su-file]").forEach((input) =>
    input.addEventListener("change", () => {
      const f = input.files[0];
      if (f) (input.dataset.suFile === "excel" ? onExcel : onReceipt)(f);
      input.value = "";
    }));
  $$("[data-su-drop]").forEach((zone) => {
    const input = zone.querySelector("[data-su-file]");
    zone.addEventListener("dragover", (e) => { e.preventDefault(); zone.classList.add("border-[#2563eb]", "bg-[#eff5ff]"); });
    zone.addEventListener("dragleave", () => zone.classList.remove("border-[#2563eb]", "bg-[#eff5ff]"));
    zone.addEventListener("drop", (e) => {
      e.preventDefault();
      zone.classList.remove("border-[#2563eb]", "bg-[#eff5ff]");
      const f = e.dataTransfer.files?.[0];
      if (f) (input.dataset.suFile === "excel" ? onExcel : onReceipt)(f);
    });
  });
  $("[data-su-template]").addEventListener("click", () => {
    const csv = "﻿date,menu,quantity,amount\n2025-10-13,아메리카노,3,13500\n2025-10-13,크루아상,2,8400\n";
    const href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const a = document.createElement("a");
    a.href = href;
    a.download = "bizgent_매출_양식.csv";
    a.click();
    URL.revokeObjectURL(href);
  });
  $$("[data-su-tab]").forEach((b) => b.addEventListener("click", () => { state.tab = b.dataset.suTab; render(); }));

  /* ---------- 단계 이동 ---------- */
  async function next() {
    // 올린 내용을 바꾸지 않고 돌아왔다면 2단계에서 고친 내용을 유지한다.
    if (state.rowsSource === sourceKey() && state.rows.length) { state.step = 2; renderRows(); return render(); }
    let raw = [];
    if (state.tab === "excel" && state.file) {
      state.busy = "parse";
      render();
      const body = new FormData();
      body.append("file", state.file);
      raw = await fetch("/api/sales/parse", { method: "POST", body }).then((r) => r.json());
      state.busy = null;
    } else if (state.tab === "receipt" && state.receipt?.rows) {
      raw = state.receipt.rows;
    } else {
      raw = manualValid().map((r) => ({ date: r.date, menuName: productName(r.productId), productId: r.productId, quantity: Number(r.quantity), amount: digits(r.amount) }));
    }
    state.rows = raw.map((r) => ({ id: uid(), date: r.date, menuName: r.menuName, productId: r.productId, quantity: r.quantity, amount: r.amount, editing: false }));
    state.rowsSource = sourceKey();
    state.step = 2;
    renderRows();
    $("[data-su-rows]").scrollTop = 0;
    render();
  }

  async function analyze() {
    state.busy = "analyze";
    render();
    const [result] = await Promise.all([
      BG.postJson("/api/sales/commit", {
        source: state.tab,
        rows: state.rows.map((r) => ({ date: r.date, productId: r.productId, quantity: r.quantity, amount: r.amount })),
      }),
      new Promise((r) => setTimeout(r, 1500)),
    ]);
    state.committed = result.count;
    state.dirtyCommit = true;
    state.busy = null;
    state.step = 3;
    render();
  }

  function close() {
    if (state.busy) return;
    if (state.dirtyCommit) {
      // 반영된 매출로 분석 화면을 다시 그린다.
      sessionStorage.setItem("bizgent-toast", "매출 데이터가 반영되었어요");
      location.href = location.pathname + location.search;
      return;
    }
    BG.closeModal(modal);
  }

  $("[data-su-next]").addEventListener("click", () => (state.step === 1 ? next() : state.step === 2 ? analyze() : close()));
  $("[data-su-back]").addEventListener("click", () => {
    if (state.step === 2) { state.step = 1; render(); } else close();
  });
  $("[data-su-close]").addEventListener("click", close);
  modal.addEventListener("mousedown", (e) => { if (e.target === modal) close(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !modal.hidden) close(); });

  // 열 때마다 처음 단계부터
  modal.addEventListener("modal:open", () => {
    Object.assign(state, { step: 1, tab: "excel", file: null, receipt: null, manual: [], rows: [], rowsSource: "", busy: null, committed: 0, dirtyCommit: false });
    renderManual();
    render();
  });

  try {
    const toast = sessionStorage.getItem("bizgent-toast");
    if (toast) { sessionStorage.removeItem("bizgent-toast"); document.addEventListener("DOMContentLoaded", () => BG.toast(toast)); }
  } catch { /* 저장소를 쓸 수 없는 환경 */ }
  render();
})();
