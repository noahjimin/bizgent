/*
 * BizGent 공통 스크립트
 * - 모달 열기/닫기, 토스트, 확인 팝업, 구독 안내, 설정 메뉴
 * 화면별 스크립트는 window.BG의 함수를 사용한다.
 */
(function () {
  const BG = (window.BG = {});

  /* ---------- 모달 ---------- */
  // data-modal-open="id" 버튼으로 열고, data-modal-close 버튼·배경 클릭·Esc로 닫는다.
  // 열면 팝업 안으로 포커스를 옮기고 Tab을 팝업 안에 가두며, 닫으면 연 버튼으로 돌려준다.
  const FOCUSABLE = 'button:not([disabled]), a[href], input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled]), summary, [tabindex]:not([tabindex="-1"])';
  const focusables = (el) => Array.from(el.querySelectorAll(FOCUSABLE)).filter((f) => f.offsetParent !== null);

  BG.openModal = function (id) {
    const el = document.getElementById(id);
    if (!el) return;
    el._opener = document.activeElement;
    el.hidden = false;
    el.dispatchEvent(new CustomEvent("modal:open"));
    const first = focusables(el)[0];
    if (first) first.focus();
  };

  BG.closeModal = function (el) {
    if (typeof el === "string") el = document.getElementById(el);
    if (!el || el.hidden) return;
    el.hidden = true;
    el.dispatchEvent(new CustomEvent("modal:close"));
    if (el._opener && el._opener.isConnected) el._opener.focus();
    el._opener = null;
  };

  document.addEventListener("keydown", (e) => {
    if (e.key !== "Tab") return;
    const open = Array.from(document.querySelectorAll("[data-modal]")).filter((m) => !m.hidden);
    const top = open[open.length - 1];
    if (!top) return;
    const items = focusables(top);
    if (!items.length) return;
    const first = items[0];
    const last = items[items.length - 1];
    if (e.shiftKey && (document.activeElement === first || !top.contains(document.activeElement))) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && (document.activeElement === last || !top.contains(document.activeElement))) {
      e.preventDefault();
      first.focus();
    }
  });

  document.addEventListener("click", (e) => {
    const opener = e.target.closest("[data-modal-open]");
    if (opener) {
      e.preventDefault();
      BG.openModal(opener.dataset.modalOpen);
      return;
    }
    const closer = e.target.closest("[data-modal-close]");
    if (closer) {
      e.preventDefault();
      BG.closeModal(closer.dataset.modalClose ? closer.dataset.modalClose : closer.closest("[data-modal]"));
    }
  });

  document.addEventListener("mousedown", (e) => {
    const backdrop = e.target;
    if (backdrop.matches && backdrop.matches("[data-backdrop-close]") && e.target === backdrop) {
      if (backdrop.id === "confirm-dialog") confirmCancel();
      else BG.closeModal(backdrop);
    }
  });

  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    const confirmEl = document.getElementById("confirm-dialog");
    if (confirmEl && !confirmEl.hidden) return confirmCancel();
    const open = Array.from(document.querySelectorAll("[data-modal]")).filter((m) => !m.hidden && m.dataset.escape !== "off");
    if (open.length) BG.closeModal(open[open.length - 1]);
    closeSettingsMenu();
    closeNotificationMenu();
  });

  /* ---------- 토스트 ---------- */
  let toastTimer;
  BG.toast = function (message, ms) {
    const el = document.getElementById("toast");
    if (!el) return;
    el.querySelector("[data-toast-message]").textContent = message;
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (el.hidden = true), ms || 2500);
  };

  /* ---------- 구독 ---------- */
  // 모든 "플랜 업그레이드" 버튼의 단일 진입점: 구독 관리 화면(SCR-U-010)으로 보낸다. 실제 전환은 그 화면의 구독하기 폼이 한다.
  BG.requestSubscribe = function () {
    location.href = "/subscription";
  };

  let hintTimer;
  BG.upgradeHint = function (feature) {
    const el = document.getElementById("upgrade-hint");
    if (!el) return;
    el.querySelector("[data-upgrade-feature]").textContent = feature;
    el.hidden = false;
    clearTimeout(hintTimer);
    hintTimer = setTimeout(() => (el.hidden = true), 3000);
  };

  document.addEventListener("click", (e) => {
    if (e.target.closest("[data-subscribe]")) {
      e.preventDefault();
      BG.requestSubscribe();
    }
    const locked = e.target.closest("[data-upgrade-feature-name]");
    if (locked) {
      e.preventDefault();
      BG.upgradeHint(locked.dataset.upgradeFeatureName);
    }
  });

  /* ---------- 확인 팝업 ---------- */
  let confirmHandler = null;
  BG.confirm = function (message, confirmLabel, onConfirm, cancelLabel) {
    const el = document.getElementById("confirm-dialog");
    el.querySelector("#confirm-dialog-message").textContent = message;
    el.querySelector("[data-confirm-ok]").textContent = confirmLabel || "확인";
    el.querySelector("[data-confirm-cancel]").textContent = cancelLabel || "취소";
    confirmHandler = onConfirm;
    el.hidden = false;
  };
  function confirmCancel() {
    const el = document.getElementById("confirm-dialog");
    if (el) el.hidden = true;
    confirmHandler = null;
  }
  document.addEventListener("click", (e) => {
    if (e.target.closest("[data-confirm-cancel]")) confirmCancel();
    if (e.target.closest("[data-confirm-ok]")) {
      const handler = confirmHandler;
      confirmCancel();
      if (handler) handler();
    }
  });

  // <form data-confirm="메시지" data-confirm-label="삭제"> : 제출 전에 확인 팝업을 띄운다.
  document.addEventListener("submit", (e) => {
    const form = e.target;
    if (!form.dataset.confirm || form.dataset.confirmed) return;
    e.preventDefault();
    BG.confirm(form.dataset.confirm, form.dataset.confirmLabel, () => {
      form.dataset.confirmed = "1";
      form.requestSubmit ? form.requestSubmit() : form.submit();
    });
  });

  /* ---------- 설정 메뉴 ---------- */
  // 사이드바 "설정"과 상단 프로필 버튼이 같은 메뉴 하나를 공유한다.
  const MENU_W = 240;
  const SUB_W = 240;
  let menuAnchor = null;

  function closeSettingsMenu() {
    const menu = document.getElementById("settings-menu");
    if (!menu) return;
    menu.hidden = true;
    menuAnchor = null;
    toggleSubmenu(false);
  }

  function toggleSubmenu(open) {
    const sub = document.querySelector("[data-settings-sub]");
    const btn = document.querySelector("[data-settings-sub-toggle]");
    if (!sub) return;
    sub.hidden = !open;
    btn.classList.toggle("bg-white/10", open);
    btn.setAttribute("aria-expanded", String(open));
  }

  function openSettingsMenu(anchor) {
    const menu = document.getElementById("settings-menu");
    if (!menu) return;
    if (menuAnchor === anchor) return closeSettingsMenu();
    menuAnchor = anchor;
    const r = anchor.getBoundingClientRect();
    const placement = anchor.dataset.settingsMenu;
    let left;
    menu.style.top = menu.style.bottom = "";
    if (placement === "above") {
      left = r.left;
      menu.style.bottom = window.innerHeight - r.top + 8 + "px";
    } else {
      left = Math.max(8, r.right - MENU_W);
      menu.style.top = r.bottom + 8 + "px";
    }
    menu.style.left = left + "px";
    const subLeft = left + MENU_W + SUB_W + 8 > window.innerWidth;
    const wrap = menu.querySelector("[data-settings-sub]");
    if (wrap) {
      wrap.classList.toggle("right-full", subLeft);
      wrap.classList.toggle("pr-[8px]", subLeft);
      wrap.classList.toggle("left-full", !subLeft);
      wrap.classList.toggle("pl-[8px]", !subLeft);
    }
    menu.hidden = false;
  }

  document.addEventListener("click", (e) => {
    const anchor = e.target.closest("[data-settings-menu]");
    if (anchor) {
      e.preventDefault();
      openSettingsMenu(anchor);
      return;
    }
    const menu = document.getElementById("settings-menu");
    if (!menu || menu.hidden) return;
    if (e.target.closest("[data-settings-sub-toggle]")) {
      toggleSubmenu(document.querySelector("[data-settings-sub]").hidden);
      return;
    }
    if (e.target.closest("[data-settings-close]")) return closeSettingsMenu();
    if (!menu.contains(e.target)) closeSettingsMenu();
  });
  document.addEventListener("mouseover", (e) => {
    const menu = document.getElementById("settings-menu");
    if (!menu || menu.hidden) return;
    const subArea = e.target.closest("[data-settings-sub-area]");
    if (subArea) toggleSubmenu(true);
    else if (e.target.closest("[role=menuitem]") && menu.contains(e.target) && !e.target.closest("[data-settings-sub]")) toggleSubmenu(false);
  });
  window.addEventListener("resize", closeSettingsMenu);

  /* ---------- 알림 목록 ---------- */
  // 사이드바와 상단바의 알림 버튼이 같은 목록 하나를 공유한다.
  const NOTIFICATION_W = 320;
  let notificationAnchor = null;

  function closeNotificationMenu() {
    const menu = document.getElementById("notification-menu");
    if (!menu) return;
    menu.hidden = true;
    notificationAnchor = null;
  }

  function openNotificationMenu(anchor) {
    const menu = document.getElementById("notification-menu");
    if (!menu) return;
    if (notificationAnchor === anchor) return closeNotificationMenu();
    notificationAnchor = anchor;
    const r = anchor.getBoundingClientRect();
    menu.style.top = menu.style.bottom = "";
    if (anchor.dataset.notificationMenu === "above") {
      menu.style.left = r.left + "px";
      menu.style.bottom = window.innerHeight - r.top + 8 + "px";
    } else {
      menu.style.left = Math.max(8, r.right - NOTIFICATION_W) + "px";
      menu.style.top = r.bottom + 8 + "px";
    }
    menu.hidden = false;
  }

  document.addEventListener("click", async (e) => {
    const anchor = e.target.closest("[data-notification-menu]");
    if (anchor) {
      e.preventDefault();
      openNotificationMenu(anchor);
      return;
    }
    const menu = document.getElementById("notification-menu");
    if (!menu || menu.hidden) return;
    if (!menu.contains(e.target)) return closeNotificationMenu();
    const item = e.target.closest("[data-notification-id]");
    if (!item) return;
    try {
      await BG.postJson(`/api/notifications/${item.dataset.notificationId}/read`);
      location.href = "/performance?upload=open";
    } catch {
      BG.toast("알림을 열지 못했어요. 다시 시도해 주세요");
    }
  });
  window.addEventListener("resize", closeNotificationMenu);

  /* ---------- 선택 칩 ---------- */
  // <button data-chip aria-pressed="true|false"> 의 선택 모양을 바꾼다. (체크 아이콘은 [data-chip-check])
  const CHIP_ON = ["f-semibold", "border-[#2563eb]", "bg-[#2563eb]", "text-white"];
  const CHIP_OFF = ["f-medium", "border-[#d5dce7]", "bg-white", "text-[#667085]", "hover:bg-[#f7f9fc]"];
  BG.setChip = function (btn, on) {
    btn.setAttribute(btn.getAttribute("role") === "radio" ? "aria-checked" : "aria-pressed", String(on));
    btn.classList.remove(...(on ? CHIP_OFF : CHIP_ON));
    btn.classList.add(...(on ? CHIP_ON : CHIP_OFF));
    const check = btn.querySelector("[data-chip-check]");
    if (check) check.hidden = !on;
  };
  BG.isChipOn = (btn) => btn.getAttribute("aria-pressed") === "true" || btn.getAttribute("aria-checked") === "true";

  /* ---------- 이미지 다운로드 ---------- */
  // 다른 사이트 이미지는 브라우저 정책상 바로 저장되지 않을 수 있어 새 탭으로 연다.
  BG.downloadImage = async function (url, filename) {
    try {
      const res = await fetch(url, { mode: "cors" });
      if (!res.ok) throw new Error();
      const blobUrl = URL.createObjectURL(await res.blob());
      const a = document.createElement("a");
      a.href = blobUrl;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(blobUrl), 1000);
    } catch {
      window.open(url, "_blank", "noopener");
    }
  };

  /* ---------- JSON 요청 ---------- */
  BG.postJson = async function (url, body) {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body || {}),
    });
    if (!res.ok) throw new Error("요청에 실패했어요 (" + res.status + ")");
    return res.json();
  };

  /* ---------- 새 캠페인 시작 ---------- */
  // <button data-new-campaign data-goal="재고 소진 프로모션" data-product-id="3" data-channels="인스타그램" data-source="insight">
  document.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-new-campaign]");
    if (!btn) return;
    e.preventDefault();
    const form = document.getElementById("new-campaign-form");
    form.goal.value = btn.dataset.goal || "";
    form.productId.value = btn.dataset.productId || "";
    form.channels.value = btn.dataset.channels || "";
    form.source.value = btn.dataset.source || "";
    form.submit();
  });

  /* ---------- POST로 이동 ---------- */
  // 작성 중 캠페인 이어서 작성하기처럼 상태를 바꾸는 이동은 POST로 보낸다. <a href="(JS 없을 때 갈 곳)" data-post-href="/campaigns/7/resume">
  BG.postTo = function (url) {
    const form = document.createElement("form");
    form.method = "post";
    form.action = url;
    document.body.appendChild(form);
    form.submit();
  };
  document.addEventListener("click", (e) => {
    const link = e.target.closest("[data-post-href]");
    // 카드 안의 다른 버튼·링크·폼을 누른 경우는 그 동작에 맡긴다.
    const inner = e.target.closest("a, button, form");
    if (!link || (inner && inner !== link && link.contains(inner))) return;
    e.preventDefault();
    BG.postTo(link.dataset.postHref);
  });

  /* ---------- 서버에서 전달한 토스트 ---------- */
  document.addEventListener("DOMContentLoaded", () => {
    const flash = document.querySelector("[data-flash-toast]");
    if (flash && flash.textContent.trim()) BG.toast(flash.textContent.trim());
  });
})();
