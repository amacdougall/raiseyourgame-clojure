var moduleDefaults = {
  setup: function() {
    F.open("http://local.raiseyourga.me");
  },

  teardown: function() {}
};

var videoModuleDefaults = {
  setup: moduleDefaults.setup,
  teardown: function() {
    F.eval("raiseyourgame.lib.youtube.force_pause();");
  }
}

function section() {
  var name = arguments[0];
  var options = arguments[1];
  module(name, options || moduleDefaults);
}


section("home page");

test("video thumbs render", function() {
  F(".detail-link:first img")
    .attr("src", "http://i1.ytimg.com/vi/7M2_4sI6I8Y/mqdefault.jpg",
      "first video thumb had correct thumbnail image");
  F(".detail-link:first .description")
    .text("Daigo vs. Gamerbee during Evo 2012.",
      "first video thumb description was correct");
});


section("video page", videoModuleDefaults);

test("click a home page video", function() {
  F("a.detail-link:first").click();
  F("div.video").visible("video container was shown");
});
