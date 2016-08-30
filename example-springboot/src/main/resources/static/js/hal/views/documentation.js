HAL.Views.Documenation = Backbone.View.extend({
  className: 'documentation',

  render: function(url) {
    this.$el.html('<iframe height="800px" width="100%" src="' + url + '"></iframe>');
  }
});
