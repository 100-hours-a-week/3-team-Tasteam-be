function renderPromotionEdit(container, state = {}) {
	renderPromotionForm(container, { ...state, mode: 'edit' });
}

function mountPromotionEdit(state = {}) {
	return mountPromotionForm({ ...state, mode: 'edit' });
}

window.promotionEditView = {
	render: renderPromotionEdit,
	mount: mountPromotionEdit,
	unmount: () => {}
};
