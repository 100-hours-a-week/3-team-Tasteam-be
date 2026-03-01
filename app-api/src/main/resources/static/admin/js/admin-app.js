function initAdminSpa() {
    if (typeof setAuthNavigator === 'function') {
        setAuthNavigator(AdminRouter.navigate);
    }

    if (typeof setApiUnauthorizedHandler === 'function') {
        setApiUnauthorizedHandler(() => {
            AdminRouter.navigate('/admin/', { replace: true });
        });
    }

    AdminRouter.start();
}

initAdminSpa();
