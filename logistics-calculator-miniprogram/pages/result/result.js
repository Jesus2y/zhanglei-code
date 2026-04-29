Page({
  data: {
    result: null
  },

  onLoad(options) {
    if (options.data) {
      const result = JSON.parse(decodeURIComponent(options.data));
      this.setData({ result });
      wx.setNavigationBarTitle({
        title: `计算结果 - ¥${result.totalCost}`
      });
    }
  },

  onRecalculate() {
    wx.navigateBack();
  },

  onSave() {
    wx.showToast({ title: '结果已保存', icon: 'success' });
  },

  onShareAppMessage() {
    return {
      title: '跨境物流成本计算器',
      path: '/pages/input/input'
    };
  }
});
