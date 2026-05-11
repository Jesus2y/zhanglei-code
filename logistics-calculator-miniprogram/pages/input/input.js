const app = getApp();

Page({
  data: {
    length: '',
    width: '',
    height: '',
    weight: '',
    quantity: '',
    countries: [
      { code: 'US', name: '美国' },
      { code: 'DE', name: '德国' },
      { code: 'JP', name: '日本' }
    ],
    countryIndex: 0,
    shippingMethod: 'AIR',
    goodsType: 'NORMAL',
    residentialDelivery: false,
    insurance: false,
    freeCountRemaining: 3,
    isMember: false
  },

  onLoad() {
    this.ensureLoginAndRefresh();
  },

  onShow() {
    this.updateFreeCount();
  },

  ensureLoginAndRefresh() {
    app.ensureLogin()
      .then(() => this.updateFreeCount())
      .catch(() => {
        wx.showToast({ title: '登录失败，请重试', icon: 'none' });
      });
  },

  updateFreeCount() {
    const userInfo = app.globalData.userInfo;
    if (!userInfo) {
      return;
    }

    const isMember = !!userInfo.isMember;
    this.setData({
      isMember,
      freeCountRemaining: isMember ? 9999 : Math.max(0, 3 - (userInfo.calculationCountToday || 0))
    });
  },

  onLengthInput(e) {
    this.setData({ length: e.detail.value });
  },

  onWidthInput(e) {
    this.setData({ width: e.detail.value });
  },

  onHeightInput(e) {
    this.setData({ height: e.detail.value });
  },

  onWeightInput(e) {
    this.setData({ weight: e.detail.value });
  },

  onQuantityInput(e) {
    this.setData({ quantity: e.detail.value });
  },

  onCountryChange(e) {
    this.setData({ countryIndex: Number(e.detail.value) });
  },

  onShippingMethodChange(e) {
    this.setData({ shippingMethod: e.detail.value });
  },

  onGoodsTypeChange(e) {
    this.setData({ goodsType: e.detail.value });
  },

  onSpecialServicesChange(e) {
    const values = e.detail.value;
    this.setData({
      residentialDelivery: values.includes('residential'),
      insurance: values.includes('insurance')
    });
  },

  validateForm() {
    const { length, width, height, weight, quantity } = this.data;

    if (!length || !width || !height) {
      wx.showToast({ title: '请输入完整的尺寸', icon: 'none' });
      return false;
    }

    if (!weight) {
      wx.showToast({ title: '请输入重量', icon: 'none' });
      return false;
    }

    if (!quantity) {
      wx.showToast({ title: '请输入数量', icon: 'none' });
      return false;
    }

    if (parseFloat(length) <= 0 || parseFloat(width) <= 0 || parseFloat(height) <= 0) {
      wx.showToast({ title: '尺寸必须大于0', icon: 'none' });
      return false;
    }

    if (parseFloat(weight) <= 0) {
      wx.showToast({ title: '重量必须大于0', icon: 'none' });
      return false;
    }

    if (parseInt(quantity, 10) <= 0) {
      wx.showToast({ title: '数量必须大于0', icon: 'none' });
      return false;
    }

    return true;
  },

  onCalculate() {
    if (!this.validateForm()) {
      return;
    }

    if (!this.data.isMember && this.data.freeCountRemaining <= 0) {
      wx.showModal({
        title: '提示',
        content: '今日免费次数已用完，是否前往会员中心？',
        confirmText: '去看看',
        success: (res) => {
          if (res.confirm) {
            wx.switchTab({ url: '/pages/settings/settings' });
          }
        }
      });
      return;
    }

    wx.showLoading({ title: '计算中...' });

    const country = this.data.countries[this.data.countryIndex];
    const payload = {
      length: parseFloat(this.data.length),
      width: parseFloat(this.data.width),
      height: parseFloat(this.data.height),
      weight: parseFloat(this.data.weight),
      quantity: parseInt(this.data.quantity, 10),
      country: country.code,
      shippingMethod: this.data.shippingMethod,
      goodsType: this.data.goodsType,
      residentialDelivery: this.data.residentialDelivery,
      insurance: this.data.insurance
    };

    app.request({
      url: `${app.globalData.apiBaseUrl}/calculate`,
      method: 'POST',
      data: payload
    }).then((resData) => {
      wx.hideLoading();
      if (resData && resData.totalCost !== undefined) {
        if (app.globalData.userInfo && !app.globalData.userInfo.isMember) {
          app.globalData.userInfo.calculationCountToday = (app.globalData.userInfo.calculationCountToday || 0) + 1;
          wx.setStorageSync('userInfo', app.globalData.userInfo);
          this.updateFreeCount();
        }

        wx.navigateTo({
          url: `/pages/result/result?data=${encodeURIComponent(JSON.stringify(resData))}`
        });
      } else {
        wx.showToast({ title: (resData && resData.message) || '计算失败', icon: 'none' });
      }
    }).catch((err) => {
      wx.hideLoading();
      wx.showToast({ title: err.message || '网络错误，请重试', icon: 'none' });
    });
  }
});
