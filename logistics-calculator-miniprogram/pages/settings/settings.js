const app = getApp();

Page({
  data: {
    userInfo: null,
    freeCountRemaining: 3,
    usedCount: 0
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  loadUserInfo() {
    app.ensureLogin()
      .then(() => app.request({
        url: `${app.globalData.apiBaseUrl}/user/profile`,
        method: 'GET'
      }))
      .then((profile) => {
        app.globalData.userInfo = profile;
        wx.setStorageSync('userInfo', profile);

        const usedCount = profile.calculationCountToday || 0;
        const remaining = profile.isMember ? 9999 : Math.max(0, 3 - usedCount);

        this.setData({
          userInfo: profile,
          freeCountRemaining: remaining,
          usedCount
        });
      })
      .catch(() => {
        wx.showToast({ title: '用户信息加载失败', icon: 'none' });
      });
  },

  onUpgrade() {
    app.request({
      url: `${app.globalData.apiBaseUrl}/payment/create`,
      method: 'POST',
      data: { plan: 'MONTHLY_MEMBER' }
    }).then((res) => {
      wx.showModal({
        title: '会员支付',
        content: `订单已创建：${res.orderNo}，金额¥${res.amount}`,
        showCancel: false
      });
    }).catch(() => {
      wx.showToast({ title: '支付接口暂不可用', icon: 'none' });
    });
  },

  onHistory() {
    wx.navigateTo({ url: '/pages/history/history' });
  },

  onHelp() {
    wx.navigateTo({ url: '/pages/help/help' });
  },

  onFeedback() {
    wx.showModal({
      title: '意见反馈',
      content: '请发送邮件到 support@logistics-calc.com',
      showCancel: false
    });
  },

  onContact() {
    wx.makePhoneCall({
      phoneNumber: '400-123-4567'
    });
  },

  onBindPhone() {
    wx.showModal({
      title: '绑定手机号',
      content: '是否授权获取您的手机号？',
      confirmText: '授权',
      success: (res) => {
        if (res.confirm) {
          this.getPhoneNumber();
        }
      }
    });
  },

  getPhoneNumber() {
    wx.showLoading({ title: '绑定中...' });

    app.request({
      url: `${app.globalData.apiBaseUrl}/user/bind-phone`,
      method: 'POST',
      data: {
        phoneNumber: this.data.userInfo.phoneNumber || ''
      }
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '绑定成功', icon: 'success' });
      this.loadUserInfo();
    }).catch((err) => {
      wx.hideLoading();
      wx.showToast({ title: err.message || '绑定失败', icon: 'none' });
    });
  }
});
