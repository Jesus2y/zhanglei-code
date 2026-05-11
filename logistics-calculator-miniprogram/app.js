App({
  globalData: {
    userInfo: null,
    // 开发环境使用本地地址，生产环境改为实际域名
    apiBaseUrl: 'http://localhost:8080/api/v1',
    // apiBaseUrl: 'https://logistics-calc.com/api/v1',  // 生产环境地址
    freeCountRemaining: 3
  },

  onLaunch() {
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo && userInfo.openid) {
      this.globalData.userInfo = userInfo;
    }
  },

  login(profile = {}) {
    return new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          if (!res.code) {
            reject(new Error('wx.login 未返回 code'));
            return;
          }

          wx.request({
            url: `${this.globalData.apiBaseUrl}/user/login`,
            method: 'POST',
            data: {
              code: res.code,
              nickname: profile.nickname || '',
              avatarUrl: profile.avatarUrl || '',
              phoneNumber: profile.phoneNumber || ''
            },
            success: (response) => {
              if (response.data && response.data.success && response.data.user) {
                this.globalData.userInfo = response.data.user;
                wx.setStorageSync('userInfo', response.data.user);
                resolve(response.data.user);
              } else {
                reject(new Error((response.data && response.data.message) || '登录失败'));
              }
            },
            fail: reject
          });
        },
        fail: reject
      });
    });
  },

  ensureLogin() {
    if (this.globalData.userInfo && this.globalData.userInfo.openid) {
      return Promise.resolve(this.globalData.userInfo);
    }

    return this.login();
  },

  request(options) {
    return this.ensureLogin().then((userInfo) => new Promise((resolve, reject) => {
      console.log('发起请求:', options.url);  // 添加日志
      wx.request({
        ...options,
        header: {
          ...(options.header || {}),
          'X-User-OpenID': userInfo.openid
        },
        timeout: 10000,  // 设置超时时间为10秒
        success: (res) => {
          console.log('请求成功:', res);  // 添加日志
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(res.data);
          } else {
            reject(new Error(`请求失败: ${res.statusCode}`));
          }
        },
        fail: (err) => {
          console.error('请求失败:', err);  // 添加错误日志
          reject(new Error(err.errMsg || '网络请求失败'));
        }
      });
    }));
  }
});
