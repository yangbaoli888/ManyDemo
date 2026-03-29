const toast = document.getElementById('toast');
const eventList = document.getElementById('eventList');
const refreshBtn = document.getElementById('refreshBtn');

const labels = {
  STOOL: '💩 大便',
  URINE: '💧 小便',
  FORMULA: '🍼 奶粉',
  BREASTFEEDING: '🤱 母乳'
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
      showToast('请输入奶粉毫升数', true);
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
  await loadEvents();
}

function formatEvent(event) {
  const date = new Date(event.happenedAt);
  const localTime = date.toLocaleString('zh-CN', { hour12: false });
  const extra = event.type === 'FORMULA'
    ? `，${event.amountMl} ml`
    : event.type === 'BREASTFEEDING'
      ? `，${event.durationMinutes} 分钟`
      : '';

  return `${labels[event.type] || event.type} · ${localTime}${extra}`;
}

async function loadEvents() {
  const response = await fetch('/api/events');
  const items = await response.json();
  eventList.innerHTML = '';

  if (!items.length) {
    const empty = document.createElement('li');
    empty.textContent = '还没有记录，开始第一条吧！';
    eventList.appendChild(empty);
    return;
  }

  items.forEach(event => {
    const li = document.createElement('li');
    li.textContent = formatEvent(event);
    eventList.appendChild(li);
  });
}

document.querySelectorAll('.action[data-type]').forEach(button => {
  button.addEventListener('click', () => record(button.dataset.type));
});

refreshBtn.addEventListener('click', loadEvents);

loadEvents();
