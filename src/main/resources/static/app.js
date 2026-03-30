const toast = document.getElementById('toast');
const timelineContainer = document.getElementById('timelineContainer');
const refreshBtn = document.getElementById('refreshBtn');

const typeStyleMap = {
  STOOL: { icon: '💩', label: '大便', iconClass: 'icon-stool', badge: '' },
  URINE: { icon: '💧', label: '小便', iconClass: 'icon-urine', badge: '' },
  FORMULA: { icon: '🍼', label: '奶粉', iconClass: 'icon-formula', badge: 'ml' },
  BREASTFEEDING: { icon: '🤱', label: '母乳', iconClass: 'icon-breast', badge: '分钟' }
};

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.style.color = isError ? '#d3366a' : '#2274d3';
}

async function record(type) {
  const payload = { type };

  if (type === 'FORMULA') {
    const amount = Number(document.getElementById('formulaAmount').value);
    if (!amount || amount < 1) {
      showToast('请选择奶粉毫升数', true);
      return;
    }
    if (amount % 30 !== 0) {
      showToast('奶粉毫升数必须是30的倍数', true);
      return;
    }
    payload.amountMl = amount;
  }

  if (type === 'BREASTFEEDING') {
    const duration = Number(document.getElementById('breastDuration').value);
    if (!duration || duration < 1) {
      showToast('请输入母乳时长（分钟）', true);
      return;
    }
    payload.durationMinutes = duration;
  }

  const response = await fetch('/api/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  const data = await response.json();
  if (!response.ok) {
    showToast(data.message || '记录失败', true);
    return;
  }

  showToast('记录成功 ✅');
  await loadTimeline();
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
    details = `${event.amountMl} ml`;
  }
  if (event.type === 'BREASTFEEDING') {
    details = `${event.durationMinutes} 分钟`;
  }

  const wrapper = document.createElement('div');
  wrapper.className = 'event-item';
  wrapper.innerHTML = `
    <div class="event-left">
      <span class="icon-pill ${style.iconClass}">${style.icon}</span>
      <span>${style.label}${details ? ` · ${details}` : ''}</span>
    </div>
    <span class="event-meta">${formatTime(event.happenedAt)}</span>
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

async function loadTimeline() {
  const response = await fetch('/api/events/timeline');
  const items = await response.json();
  renderTimeline(items);
}

document.querySelectorAll('.action[data-type]').forEach(button => {
  button.addEventListener('click', () => record(button.dataset.type));
});

refreshBtn.addEventListener('click', loadTimeline);

loadTimeline();
