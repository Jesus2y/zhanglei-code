const app = getApp();

Page({
  data: {
    userInfo: null,
    freeCountRemaining: 3,
    usedCount: 0,
    isLoading: false  // 添加加载状态
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    // 避免重复加载：只有当userInfo为空时才加载
    if (!this.data.userInfo) {
      this.loadUserInfo();
    }
  },

  loadUserInfo() {
    // 防止重复请求
    if (this.data.isLoading) {
      console.log('正在加载中，跳过重复请求');
      return;
    }

    this.setData({ isLoading: true });
    wx.showLoading({ title: '加载中...' });

    console.log('开始加载用户信息...');
    
    app.ensureLogin()
      .then(() => {
        console.log('登录成功，开始请求用户资料...');
        return app.request({
          url: `${app.globalData.apiBaseUrl}/user/profile`,
          method: 'GET'
        });
      })
      .then((profile) => {
        console.log('用户信息加载成功:', profile);
        
        app.globalData.userInfo = profile;
        wx.setStorageSync('userInfo', profile);

        const usedCount = profile.calculationCountToday || 0;
        const remaining = profile.isMember ? 9999 : Math.max(0, 3 - usedCount);

        this.setData({
          userInfo: profile,
          freeCountRemaining: remaining,
          usedCount,
          isLoading: false
        });
        
        wx.hideLoading();
      })
      .catch((err) => {
        console.error('加载用户信息失败:', err);
        this.setData({ isLoading: false });
        wx.hideLoading();
        wx.showToast({ 
          title: err.message || '用户信息加载失败', 
          icon: 'none',
          duration: 2000
        });
      });
  },

  onUpgrade() {
    if (this.data.isLoading) return;

    this.setData({ isLoading: true });
    wx.showLoading({ title: '正在创建订单...' });

    app.request({
      url: `${app.globalData.apiBaseUrl}/payment/create`,
      method: 'POST'
    }).then((res) => {
      wx.hideLoading();
      this.setData({ isLoading: false });

      if (!res.success) {
        wx.showToast({ title: res.message || '订单创建失败', icon: 'none' });
        return;
      }

      // 调起微信支付
      wx.requestPayment({
        appId: res.appId,
        timeStamp: String(res.timeStamp),
        nonceStr: res.nonceStr,
        package: res.packageValue,  // 后端返回的 packageValue
        signType: 'RSA',
        paySign: res.paySign,

        success(payRes) {
          wx.showToast({ title: '支付成功', icon: 'success' });
          // 支付成功后刷新用户状态
          setTimeout(() => {
            app.globalData.userInfo.isMember = true;
            wx.setStorageSync('userInfo', app.globalData.userInfo);
            const pages = getCurrentPages();
            const currentPage = pages[pages.length - 1];
            if (currentPage && currentPage.loadUserInfo) {
              currentPage.loadUserInfo();
            }
          }, 1500);
        },

        fail(err) {
          // 用户取消支付或支付失败
          console.log('支付失败/取消:', err);
          if (err.errMsg && !err.errMsg.includes('cancel')) {
            wx.showToast({ title: '支付失败，请重试', icon: 'none' });
          }
        },

        complete() {
          // 无论成功失败都重置状态
          this.setData({ isLoading: false });
        }
      });
    }).catch((err) => {
      this.setData({ isLoading: false });
      wx.hideLoading();
      wx.showToast({ title: err.message || '支付接口异常', icon: 'none' });
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
