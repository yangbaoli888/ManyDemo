const toast = document.getElementById('toast');
const timelineContainer = document.getElementById('timelineContainer');
const statsContainer = document.getElementById('statsContainer');
const refreshBtn = document.getElementById('refreshBtn');
const formulaBtn = document.getElementById('formulaBtn');
const breastBtn = document.getElementById('breastBtn');
const weightBtn = document.getElementById('weightBtn');
const formulaAmountWrap = document.getElementById('formulaAmountWrap');
const breastAmountWrap = document.getElementById('breastAmountWrap');

const typeStyleMap = {
  STOOL: { icon: '💩', label: '大便', iconClass: 'icon-stool' },
  URINE: { icon: '💧', label: '小便', iconClass: 'icon-urine' },
  FORMULA: { icon: '🍼', label: '奶粉', iconClass: 'icon-formula' },
  BREASTFEEDING: { icon: '🤱', label: '母乳', iconClass: 'icon-breast' },
  WEIGHT: { icon: '⚖️', label: '体重', iconClass: 'icon-weight' }
};

let activeStatus = { formulaStartedAt: null, breastfeedingStartedAt: null };

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.style.color = isError ? '#d3366a' : '#2274d3';
}

async function postJson(url, payload) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    throw new Error((data && data.message) || '操作失败');
  }
  return data;
}

async function quickRecord(type) {
  try {
    await postJson('/api/events/quick', { type });
    showToast('记录成功 ✅');
    await loadTimeline();
    await loadStats();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function recordWeight() {
  const grams = Number(document.getElementById('weightInput').value);
  if (!grams || grams < 1) {
    showToast('请输入有效体重（克）', true);
    return;
  }

  try {
    await postJson('/api/events/weight', { weightGrams: grams });
    showToast('体重记录成功 ✅');
    document.getElementById('weightInput').value = '';
    await loadTimeline();
    await loadStats();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function start(type) {
  try {
    await postJson('/api/events/start', { type });
    showToast(`已开始${type === 'FORMULA' ? '奶粉' : '母乳'}打卡`);
    await refreshStatus();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function end(type) {
  const payload = { type };

  if (type === 'FORMULA') {
    const amount = Number(document.getElementById('formulaAmount').value);
    if (!amount || amount < 1) {
      showToast('结束奶粉前请先选择奶粉量', true);
      return;
    }
    if (amount % 30 !== 0) {
      showToast('奶粉毫升数必须是30的倍数', true);
      return;
    }
    payload.amountMl = amount;
  }

  if (type === 'BREASTFEEDING') {
    const optionalAmount = Number(document.getElementById('breastAmount').value);
    if (optionalAmount > 0) {
      payload.amountMl = optionalAmount;
    }
  }

  try {
    await postJson('/api/events/end', payload);
    showToast(`已结束${type === 'FORMULA' ? '奶粉' : '母乳'}打卡`);
    if (type === 'FORMULA') {
      document.getElementById('formulaAmount').value = '';
    }
    if (type === 'BREASTFEEDING') {
      document.getElementById('breastAmount').value = '';
    }
    await refreshStatus();
    await loadTimeline();
    await loadStats();
  } catch (err) {
    showToast(err.message, true);
  }
}

async function refreshStatus() {
  const response = await fetch('/api/events/status');
  activeStatus = await response.json();

  const formulaActive = !!activeStatus.formulaStartedAt;
  formulaBtn.textContent = formulaActive ? '结束奶粉' : '开始奶粉';
  formulaAmountWrap.classList.toggle('hidden', !formulaActive);

  const breastActive = !!activeStatus.breastfeedingStartedAt;
  breastBtn.textContent = breastActive ? '结束母乳' : '开始母乳';
  breastAmountWrap.classList.toggle('hidden', !breastActive);
}

function formatTime(isoTime) {
  return new Date(isoTime).toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatDate(dateText) {
  const date = new Date(`${dateText}T00:00:00`);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  });
}

function renderEventItem(event) {
  const style = typeStyleMap[event.type] || { icon: '📝', label: event.type, iconClass: '' };
  let details = '';

  if (event.type === 'FORMULA') {
    details = `开始 ${formatTime(event.startedAt)} · 结束 ${formatTime(event.endedAt)} · ${event.amountMl} ml`;
  } else if (event.type === 'BREASTFEEDING') {
    const ml = event.amountMl ? ` · ${event.amountMl} ml` : '';
    details = `开始 ${formatTime(event.startedAt)} · 结束 ${formatTime(event.endedAt)} · ${event.durationMinutes} 分钟${ml}`;
  } else if (event.type === 'WEIGHT') {
    details = `记录时间 ${formatTime(event.happenedAt)} · ${event.weightGrams} g`;
  } else {
    details = `打卡时间 ${formatTime(event.happenedAt)}`;
  }

  const wrapper = document.createElement('div');
  wrapper.className = 'event-item';
  wrapper.innerHTML = `
    <div class="event-left">
      <span class="icon-pill ${style.iconClass}">${style.icon}</span>
      <span>${style.label}</span>
    </div>
    <span class="event-meta">${details}</span>
  `;

  return wrapper;
}

function renderTimeline(days) {
  timelineContainer.innerHTML = '';

  if (!days.length) {
    timelineContainer.innerHTML = '<div class="day-group">还没有记录，开始第一条吧！</div>';
    return;
  }

  days.forEach(day => {
    const group = document.createElement('section');
    group.className = 'day-group';

    const title = document.createElement('h3');
    title.className = 'day-title';
    title.textContent = formatDate(day.date);
    group.appendChild(title);

    day.events.forEach(event => group.appendChild(renderEventItem(event)));
    timelineContainer.appendChild(group);
  });
}

function renderStats(days) {
  statsContainer.innerHTML = '';

  if (!days.length) {
    statsContainer.innerHTML = '<div class="stats-item">暂无统计数据</div>';
    return;
  }

  days.forEach(day => {
    const div = document.createElement('div');
    div.className = 'stats-item';
    div.innerHTML = `
      <strong>${formatDate(day.date)}</strong><br>
      💩 大便次数：${day.stoolCount}<br>
      💧 小便次数：${day.urineCount}<br>
      🍼 奶粉总量：${day.formulaTotalMl} ml<br>
      🤱 母乳总时长：${day.breastfeedingTotalMinutes} 分钟<br>
      🤱 母乳总量：${day.breastfeedingTotalMl} ml<br>
      ⚖️ 体重记录次数：${day.weightCount}${day.latestWeightGrams ? `（最新 ${day.latestWeightGrams} g）` : ''}
    `;
    statsContainer.appendChild(div);
  });
}

async function loadTimeline() {
  const response = await fetch('/api/events/timeline');
  const items = await response.json();
  renderTimeline(items);
}

async function loadStats() {
  const response = await fetch('/api/events/stats');
  const items = await response.json();
  renderStats(items);
}

document.querySelectorAll('.action[data-quick-type]').forEach(button => {
  button.addEventListener('click', () => quickRecord(button.dataset.quickType));
});

weightBtn.addEventListener('click', recordWeight);

formulaBtn.addEventListener('click', async () => {
  if (activeStatus.formulaStartedAt) {
    await end('FORMULA');
  } else {
    await start('FORMULA');
  }
});

breastBtn.addEventListener('click', async () => {
  if (activeStatus.breastfeedingStartedAt) {
    await end('BREASTFEEDING');
  } else {
    await start('BREASTFEEDING');
  }
});

refreshBtn.addEventListener('click', async () => {
  await refreshStatus();
  await loadTimeline();
  await loadStats();
});

(async function init() {
  await refreshStatus();
  await loadTimeline();
  await loadStats();
})();
