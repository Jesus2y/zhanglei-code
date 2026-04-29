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
    const userInfo = app.globalData.userInfo;
    if (userInfo) {
      const usedCount = userInfo.calculationCountToday || 0;
      const remaining = userInfo.isMember ? 999 : Math.max(0, 3 - usedCount);
      
      this.setData({
        userInfo,
        freeCountRemaining: remaining,
        usedCount
      });
    } else {
      app.login().then(() => {
        this.loadUserInfo();
      });
    }
  },

  onUpgrade() {
    wx.showModal({
      title: '升级会员',
      content: '支付¥9.9成为会员，享受无限次计算服务',
      confirmText: '确认支付',
      success: (res) => {
        if (res.confirm) {
          wx.showToast({ title: '功能开发中', icon: 'none' });
        }
      }
    });
  },

  onHistory() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  onHelp() {
    wx.navigateTo({
      url: '/pages/help/help'
    });
  },

  onFeedback() {
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  onContact() {
    wx.makePhoneCall({
      phoneNumber: '400-123-4567'
    });
  }
});
