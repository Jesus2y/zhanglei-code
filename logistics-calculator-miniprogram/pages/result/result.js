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

      // 标题显示价格区间
      let costTitle;
      if (result.needQuote) {
        costTitle = `计算结果 - 需询价`;
      } else if (result.totalCostMin && result.totalCostMax) {
        costTitle = `计算结果 - ¥${result.totalCostMin}~${result.totalCostMax}`;
      } else {
        costTitle = `计算结果 - ¥${result.totalCost || '--'}`;
      }
      wx.setNavigationBarTitle({
        title: costTitle
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
      totalCostMin: result.totalCostMin,
      totalCostMax: result.totalCostMax,
      totalCost: result.totalCost,
      costPerItemMin: result.costPerItemMin,
      costPerItemMax: result.costPerItemMax,
      deliveryTime: result.deliveryTime,
      goodsType: result.goodsType,
      needQuote: result.needQuote,
      feeDetails: result.feeDetails || [],
      warnings: result.warnings || [],
      createdAt: new Date().toISOString()
    });
    wx.setStorageSync('calculationHistory', history.slice(0, 50));

    wx.showToast({ title: '结果已保存', icon: 'success' });
  },

  onShareAppMessage() {
    const r = this.data.result;
    if (!r) return { title: '跨境物流费用计算器', path: '/pages/input/input' };

    let shareText;
    if (r.needQuote) {
      shareText = '我刚算了物流成本，敏感货海运需单独询价';
    } else if (r.totalCostMin && r.totalCostMax) {
      shareText = `我刚算出物流成本：¥${r.totalCostMin}~${r.totalCostMax}`;
    } else {
      shareText = `我刚算出物流成本：¥${r.totalCost || '--'}`;
    }
    return {
      title: shareText,
      path: '/pages/input/input'
    };
  }
});
