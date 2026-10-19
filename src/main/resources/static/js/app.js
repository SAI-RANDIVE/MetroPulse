/*
 * MetroPulse - small progressive enhancements. Every feature also works without JavaScript.
 *  1. Station <select data-combobox> -> searchable autocomplete (keyboard accessible, ARIA combobox).
 *  2. Swap origin and destination.
 *  3. Live filter for the station directory.
 *  4. Loading feedback on submit, confirmation for risky admin actions.
 */
(function () {
    "use strict";

    const normalize = (text) => (text || "").toLowerCase().normalize("NFD").replace(/[^a-z0-9]/g, "");

    /* ---------- 1. station combobox ---------- */
    let comboCount = 0;

    function upgradeSelect(select) {
        const id = "combo-" + (++comboCount);
        const options = Array.from(select.options)
            .filter((o) => o.value)
            .map((o) => ({
                value: o.value,
                name: o.textContent.trim(),
                area: o.dataset.area || "",
                lines: o.dataset.lines || "",
                key: normalize(o.textContent + " " + (o.dataset.area || ""))
            }));

        const wrapper = document.createElement("div");
        wrapper.className = "combo";
        wrapper.innerHTML =
            '<svg class="combo-icon" width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">' +
            '<circle cx="12" cy="10" r="3.2" fill="none" stroke="currentColor" stroke-width="2"/>' +
            '<path d="M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11z" fill="none" stroke="currentColor" stroke-width="2"/></svg>';

        const input = document.createElement("input");
        input.type = "text";
        input.className = "form-control combo-input" + (select.classList.contains("is-invalid") ? " is-invalid" : "");
        input.id = select.id;
        input.placeholder = select.dataset.placeholder || "Start typing a station";
        input.autocomplete = "off";
        input.setAttribute("role", "combobox");
        input.setAttribute("aria-autocomplete", "list");
        input.setAttribute("aria-expanded", "false");
        input.setAttribute("aria-controls", id + "-list");

        const list = document.createElement("ul");
        list.className = "combo-list";
        list.id = id + "-list";
        list.setAttribute("role", "listbox");
        list.hidden = true;

        // The original select keeps the submitted value; it is hidden but stays in the form.
        select.removeAttribute("id");
        select.hidden = true;
        select.tabIndex = -1;
        select.setAttribute("aria-hidden", "true");
        select.parentNode.insertBefore(wrapper, select);
        wrapper.appendChild(input);
        wrapper.appendChild(list);
        wrapper.appendChild(select);

        const selected = select.selectedOptions[0];
        if (selected && selected.value) input.value = selected.textContent.trim();

        let matches = [];
        let active = -1;

        function render() {
            const q = normalize(input.value);
            matches = options
                .filter((o) => !q || o.key.includes(q))
                .sort((a, b) => (normalize(a.name).startsWith(q) ? 0 : 1) - (normalize(b.name).startsWith(q) ? 0 : 1))
                .slice(0, 12);
            list.innerHTML = "";
            if (!matches.length) {
                const li = document.createElement("li");
                li.className = "combo-empty";
                li.textContent = "No station matches “" + input.value + "”";
                list.appendChild(li);
            }
            matches.forEach((o, i) => {
                const li = document.createElement("li");
                li.className = "combo-option";
                li.id = id + "-opt-" + i;
                li.setAttribute("role", "option");
                li.setAttribute("aria-selected", i === active ? "true" : "false");
                const name = document.createElement("span");
                name.textContent = o.name;
                const meta = document.createElement("span");
                meta.className = "area";
                meta.textContent = [o.lines && "Line " + o.lines, o.area].filter(Boolean).join(" · ");
                li.append(name, meta);
                li.addEventListener("mousedown", (e) => { e.preventDefault(); choose(o); });
                list.appendChild(li);
            });
            open(true);
        }

        function open(show) {
            list.hidden = !show;
            input.setAttribute("aria-expanded", show ? "true" : "false");
            if (!show) input.removeAttribute("aria-activedescendant");
        }

        function choose(o) {
            select.value = o.value;
            input.value = o.name;
            input.classList.remove("is-invalid");
            select.dispatchEvent(new Event("change", {bubbles: true}));
            open(false);
        }

        function highlight(index) {
            active = Math.max(0, Math.min(index, matches.length - 1));
            Array.from(list.children).forEach((li, i) => li.setAttribute("aria-selected", i === active ? "true" : "false"));
            const li = list.children[active];
            if (li) {
                input.setAttribute("aria-activedescendant", li.id);
                li.scrollIntoView({block: "nearest"});
            }
        }

        input.addEventListener("focus", () => { active = -1; render(); input.select(); });
        input.addEventListener("input", () => { select.value = ""; active = 0; render(); highlight(0); });
        input.addEventListener("blur", () => {
            open(false);
            // If the text exactly names a station, accept it; otherwise restore the last valid choice.
            const exact = options.find((o) => normalize(o.name) === normalize(input.value));
            if (exact) { choose(exact); return; }
            const current = options.find((o) => o.value === select.value);
            input.value = current ? current.name : "";
        });
        input.addEventListener("keydown", (e) => {
            if (e.key === "ArrowDown") { e.preventDefault(); if (list.hidden) render(); highlight(active + 1); }
            else if (e.key === "ArrowUp") { e.preventDefault(); highlight(active - 1); }
            else if (e.key === "Enter" && !list.hidden && matches[active]) { e.preventDefault(); choose(matches[active]); }
            else if (e.key === "Escape") { open(false); }
        });

        // Lets the swap button update the visible text as well.
        select.addEventListener("metropulse:sync", () => {
            const current = options.find((o) => o.value === select.value);
            input.value = current ? current.name : "";
        });
    }

    document.querySelectorAll("select[data-combobox]").forEach(upgradeSelect);

    /* ---------- 2. swap origin / destination ---------- */
    document.querySelectorAll("[data-swap]").forEach((button) => {
        button.addEventListener("click", () => {
            const form = button.closest("form");
            const from = form.querySelector("select[name='from']");
            const to = form.querySelector("select[name='to']");
            if (!from || !to) return;
            const tmp = from.value;
            from.value = to.value;
            to.value = tmp;
            from.dispatchEvent(new Event("metropulse:sync"));
            to.dispatchEvent(new Event("metropulse:sync"));
        });
    });

    /* ---------- 3. live directory filter ---------- */
    const filter = document.querySelector("[data-live-filter]");
    if (filter) {
        const items = Array.from(document.querySelectorAll("[data-filter-item]"));
        const groups = Array.from(document.querySelectorAll("[data-filter-group]"));
        const counter = document.querySelector("[data-filter-count]");
        const emptyNote = document.querySelector("[data-filter-empty]");
        const apply = () => {
            const q = normalize(filter.value);
            let shown = 0;
            items.forEach((item) => {
                const visible = !q || normalize(item.dataset.filterItem).includes(q);
                item.hidden = !visible;
                if (visible) shown++;
            });
            groups.forEach((g) => { g.hidden = !g.querySelector("[data-filter-item]:not([hidden])"); });
            if (counter) counter.textContent = shown + (shown === 1 ? " station" : " stations");
            if (emptyNote) emptyNote.hidden = shown !== 0;
        };
        filter.addEventListener("input", apply);
        apply();
    }

    /* ---------- 4. loading feedback and confirmations ---------- */
    document.querySelectorAll("form[data-loading]").forEach((form) => {
        form.addEventListener("submit", () => {
            const button = form.querySelector("button[type='submit']");
            if (button) button.classList.add("is-loading");
        });
    });
    document.querySelectorAll("[data-confirm]").forEach((el) => {
        el.addEventListener("click", (e) => {
            if (!window.confirm(el.dataset.confirm)) e.preventDefault();
        });
    });
})();
