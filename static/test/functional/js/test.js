module("raiseyourgame", {
  setup: function() {
    F.open("http://local.raiseyourga.me");
  },

  teardown: function() {
    // TODO: base on pause readiness instead?
    console.log("waiting 5 seconds...");
    F.wait(5000, function() {
      console.log("pausing...");
      F.eval("raiseyourgame.lib.youtube.pause();");
    });
  }
});

test("click a video", function() {
  F("a.detail-link:first").click();
  F("div.video").visible("video container should be shown");
});
