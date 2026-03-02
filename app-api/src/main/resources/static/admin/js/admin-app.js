async function initAdminSpa() {
    if (typeof setAuthNavigator === 'function') {
        setAuthNavigator(AdminRouter.navigate);
    }

    if (typeof setApiUnauthorizedHandler === 'function') {
        setApiUnauthorizedHandler(() => {
            AdminRouter.navigate('/admin/', { replace: true });
        });
    }

    if (typeof window.AdminUtils?.refreshAdminFeatureFlags === 'function') {
        await window.AdminUtils.refreshAdminFeatureFlags();
    }

    window.reloadAdminFeatureFlags = async () => {
        if (typeof window.AdminUtils?.refreshAdminFeatureFlags === 'function') {
            await window.AdminUtils.refreshAdminFeatureFlags();
        }
    };

    AdminRouter.start();
}

initAdminSpa().catch((error) => {
    if (error?.message) {
        console.error('admin 앱 초기화 실패', error);
    }
});
