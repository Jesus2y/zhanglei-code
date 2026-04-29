App({
  globalData: {
    userInfo: null,
    apiBaseUrl: 'https://logistics-calc.com/api/v1',
    freeCountRemaining: 3
  },

  onLaunch() {
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.globalData.userInfo = userInfo;
    }
  },

  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (res.code) {
            wx.request({
              url: `${this.globalData.apiBaseUrl}/user/login`,
              method: 'POST',
              data: {
                code: res.code
              },
              success: (response) => {
                if (response.data.success) {
                  this.globalData.userInfo = response.data.user;
                  wx.setStorageSync('userInfo', response.data.user);
                  resolve(response.data.user);
                } else {
                  reject(new Error('登录失败'));
                }
              },
              fail: reject
            });
          }
        },
        fail: reject
      });
    });
  }
});
