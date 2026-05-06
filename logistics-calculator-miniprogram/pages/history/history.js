const app = getApp();

Page({
  data: {
    historyList: []
  },

  onShow() {
    this.loadHistory();
  },

  loadHistory() {
    app.request({
      url: `${app.globalData.apiBaseUrl}/user/history?page=0&size=20`,
      method: 'GET'
    }).then((res) => {
      if (res && res.success) {
        this.setData({ historyList: res.items || [] });
        return;
      }

      this.loadLocalHistory();
    }).catch(() => {
      this.loadLocalHistory();
    });
  },

  loadLocalHistory() {
    const historyList = wx.getStorageSync('calculationHistory') || [];
    this.setData({ historyList });
  },

  onClearHistory() {
    wx.showModal({
      title: '确认清空',
      content: '确定清空所有历史记录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('calculationHistory');
          this.setData({ historyList: [] });
          wx.showToast({ title: '已清空', icon: 'success' });
        }
      }
    });
  }
});
