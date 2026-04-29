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
    residentialDelivery: false,
    insurance: false,
    freeCountRemaining: 3
  },

  onLoad() {
    this.updateFreeCount();
  },

  onShow() {
    this.updateFreeCount();
  },

  updateFreeCount() {
    const userInfo = app.globalData.userInfo;
    if (userInfo && !userInfo.isMember) {
      this.setData({
        freeCountRemaining: Math.max(0, 3 - (userInfo.calculationCountToday || 0))
      });
    }
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
    this.setData({ countryIndex: e.detail.value });
  },

  onShippingMethodChange(e) {
    this.setData({ shippingMethod: e.detail.value });
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
    
    if (parseInt(quantity) <= 0) {
      wx.showToast({ title: '数量必须大于0', icon: 'none' });
      return false;
    }
    
    return true;
  },

  onCalculate() {
    if (!this.validateForm()) {
      return;
    }

    if (this.data.freeCountRemaining <= 0) {
      wx.showModal({
        title: '提示',
        content: '今日免费次数已用完，是否升级为会员？',
        confirmText: '升级会员',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({ url: '/pages/settings/settings' });
          }
        }
      });
      return;
    }

    wx.showLoading({ title: '计算中...' });

    const country = this.data.countries[this.data.countryIndex];
    
    wx.request({
      url: `${app.globalData.apiBaseUrl}/calculate`,
      method: 'POST',
      header: {
        'X-User-OpenID': app.globalData.userInfo.openid
      },
      data: {
        length: parseFloat(this.data.length),
        width: parseFloat(this.data.width),
        height: parseFloat(this.data.height),
        weight: parseFloat(this.data.weight),
        quantity: parseInt(this.data.quantity),
        country: country.code,
        shippingMethod: this.data.shippingMethod,
        residentialDelivery: this.data.residentialDelivery,
        insurance: this.data.insurance
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data && res.data.totalCost) {
          wx.navigateTo({
            url: `/pages/result/result?data=${encodeURIComponent(JSON.stringify(res.data))}`
          });
        } else {
          wx.showToast({ title: res.data.message || '计算失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '网络错误，请重试', icon: 'none' });
      }
    });
  }
});
