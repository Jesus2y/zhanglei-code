Page({
  data: {
    result: null
  },

  onLoad(options) {
    if (!options.data) {
      wx.showToast({ title: '结果数据缺失', icon: 'none' });
      return;
    }

    try {
      const result = JSON.parse(decodeURIComponent(options.data));
      this.setData({ result });
      wx.setNavigationBarTitle({
        title: `计算结果 - ¥${result.totalCost}`
      });
    } catch (e) {
      wx.showToast({ title: '结果解析失败', icon: 'none' });
    }
  },

  onRecalculate() {
    wx.navigateBack();
  },

  onSave() {
    const { result } = this.data;
    if (!result) {
      wx.showToast({ title: '无可保存数据', icon: 'none' });
      return;
    }

    const history = wx.getStorageSync('calculationHistory') || [];
    history.unshift({
      id: `${Date.now()}`,
      totalCost: result.totalCost,
      costPerItem: result.costPerItem,
      feeDetails: result.feeDetails || [],
      warnings: result.warnings || [],
      createdAt: new Date().toISOString()
    });
    wx.setStorageSync('calculationHistory', history.slice(0, 50));

    wx.showToast({ title: '结果已保存', icon: 'success' });
  },

  onShareAppMessage() {
    const totalCost = this.data.result ? this.data.result.totalCost : '--';
    return {
      title: `我刚算出物流成本：¥${totalCost}`,
      path: '/pages/input/input'
    };
  }
});
