$(document).ready(function () {
  $("#category-form").on("submit", function (event) {
    if (!validateForm()) {
      event.preventDefault();
      console.log("form validation failed");
    }
  });

  function validateForm() {
    var isValid = true;
    const codeError = document.getElementById("codeError");
    const nameError = document.getElementById("nameError");
    const displayError = document.getElementById("displayError");

    $(".form-control").each(function () {
      let id = $(this).attr("id");

      // codeのバリデーション
      if (id === "code") {
        let code = $(this).val();
        if (code === "" || !isPatternValid(code)) {
          $(this).addClass("is-invalid");
          isValid = false;
          
          codeError.textContent = "半角大文字アルファベット3文字 + ハイフン + 半角数字3文字を入力してください。";
        } else {
          $(this).removeClass("is-invalid");
          codeError.textContent = "";
        }
        console.log(!isPatternValid(code));
      }

      // nameのバリデーション
      if (id === "name") {
        let name = $(this).val();
        if (name === "") {
          $(this).addClass("is-invalid");
          isValid = false;
          nameError.textContent = "名前を入力してください。";
        } else if (name.length > 20) {
          $(this).addClass("is-invalid");
          isValid = false;
          nameError.textContent = "名前は20字以内で入力してください。";
        } else {
          $(this).removeClass("is-invalid");
          nameError.textContent = "";

        }
        console.log(name.length);
      }

      // desplay_orderのバリデーション
      if (id === "display_order") {
        let displayOrder = $(this).val();
        if (displayOrder === "" || displayOrder < 0 || displayOrder > 999) {
          $(this).addClass("is-invalid");
          isValid = false;
          displayError.textContent = "数字を入力してください。";

        } else {
          $(this).removeClass("is-invalid");
          displayError.textContent = "";

        }
      }
    });
    return isValid;
  }

  function isPatternValid(code) {
    let pattern = /^([A-Z]{3})-([0-9]{3})$/;
    return pattern.test(code);
  }
});
